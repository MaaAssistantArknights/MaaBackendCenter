package plus.maa.backend.service;

import cn.hutool.extra.mail.MailAccount;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import plus.maa.backend.common.bo.EmailBusinessObject;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.config.external.Mail;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.service.model.CommentNotification;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author LoMu
 * Date 2022-12-24 11:05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    @Value("${maa-copilot.vcode.expire:600}")
    private int expire;

    @Value("${maa-copilot.info.domain}")
    private String domain;

    private final MaaCopilotProperties maaCopilotProperties;

    @Value("${debug.email.no-send:false}")
    private boolean flagNoSend;

    private final RedisCache redisCache;

    private final static MailAccount mailAccount = new MailAccount();

    /**
     * 初始化发件人信息
     */
    @PostConstruct
    private void initMailAccount() {
        Mail mail = maaCopilotProperties.getMail();
        mailAccount
                .setHost(mail.getHost())
                .setPort(mail.getPort())
                .setFrom(mail.getFrom())
                .setUser(mail.getUser())
                .setPass(mail.getPass())
                .setSslEnable(mail.getSsl())
                .setStarttlsEnable(mail.getStarttls());
        log.info("发件人信息初始化完成：{}", mailAccount);
    }

    /**
     * 发送验证码
     * 以email作为 redis key
     * vcode(验证码)作为 redis value
     *
     * @param email 邮箱
     */

    @Async
    public void sendVCode(String email) {
        // 6位随机数验证码
        String vcode = RandomStringUtils.random(6, true, true).toUpperCase();
        if (flagNoSend) {
            log.debug("vcode is " + vcode);
            log.warn("Email not sent, no-send enabled");
        } else {
            EmailBusinessObject.builder()
                    .setMailAccount(mailAccount)
                    .setEmail(email)
                    .sendVerificationCodeMessage(vcode);
        }
        // 存redis
        redisCache.setCache("vCodeEmail:" + email, vcode, expire);
    }

    @Async
    public void verifyVCode(String email, String vcode) {
        String cacheVCode = redisCache.getCache("vCodeEmail:" + email, String.class);
        Assert.state(StringUtils.equalsIgnoreCase(cacheVCode, vcode), "验证码错误");
        redisCache.removeCache("vCodeEmail:" + email);
    }

    /**
     * 验证发到某个邮箱的验证码
     *
     * @param email               邮箱
     * @param vcode               验证码
     * @param clearVCodeOnSuccess 验证成功是否删除验证码
     * @return 是否一致
     */

    public boolean verifyVCode2(String email, String vcode, boolean clearVCodeOnSuccess) {
        // FIXME:可能出现多线程数据争用问题，想办法用redis的一些方法直接比较完删除
        String cacheVCode = redisCache.getCache("vCodeEmail:" + email, String.class);
        boolean result = StringUtils.equalsIgnoreCase(cacheVCode, vcode);
        if (clearVCodeOnSuccess && result) {
            redisCache.removeCache("vCodeEmail:" + email);
        }
        return result;
    }

    /**
     * @param email 发送激活验证邮箱
     */
    @Async
    public void sendActivateUrl(String email) {
        // 生成uuid作为唯一标识符
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String url = domain + "/user/activateAccount?nonce=" + uuid;
        if (flagNoSend) {
            log.debug("url is " + url);
            log.warn("Email not sent, no-send enabled");
        } else {
            EmailBusinessObject.builder()
                    .setMailAccount(mailAccount)
                    .setEmail(email)
                    .sendActivateUrlMessage(url);
        }

        redisCache.setCache("UUID:" + uuid, email, expire);
    }

    @Async
    public void sendCommentNotification(String email, CommentNotification commentNotification) {
        int limit = 25;
        //长度超过limit 后续会被替换为....
        String title = "Re: " + commentNotification.getTitle();
        if (Strings.isNotBlank(title)) {
            if (title.length() > limit) {
                title = title.substring(0, limit) + "....";
            }
        }

        Map<String, String> map = new HashMap<>();
        map.put("authorName", commentNotification.getAuthorName());
        map.put("forntEndLink", maaCopilotProperties.getInfo().getFrontendDomain());
        map.put("reName", commentNotification.getReName());
        map.put("date", commentNotification.getDate());
        map.put("title", title);
        map.put("reMessage", commentNotification.getReMessage());
        EmailBusinessObject.builder()
                .setTitle("收到新回复 来自用户@" + commentNotification.getReName() + title)
                .setMailAccount(mailAccount)
                .setEmail(email)
                .sendCommentNotification(map);

    }
}
