package plus.maa.backend.service;

import cn.hutool.extra.mail.MailAccount;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.bo.EmailBusinessObject;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.config.external.Mail;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.service.model.CommentNotification;

import java.util.HashMap;
import java.util.Map;

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

    private final MailAccount MAIL_ACCOUNT = new MailAccount();

    // 注入自身代理类的延迟加载代理类
    @Lazy
    @Resource
    private EmailService emailService;

    /**
     * 初始化邮件账户信息
     */
    @PostConstruct
    private void initMailAccount() {
        Mail mail = maaCopilotProperties.getMail();
        MAIL_ACCOUNT
                .setHost(mail.getHost())
                .setPort(mail.getPort())
                .setFrom(mail.getFrom())
                .setUser(mail.getUser())
                .setPass(mail.getPass())
                .setSslEnable(mail.getSsl())
                .setStarttlsEnable(mail.getStarttls());

        log.info("邮件账户信息初始化完成: {}", MAIL_ACCOUNT);
    }

    /**
     * 发送验证码
     * 以email作为 redis key
     * vcode(验证码)作为 redis value
     *
     * @param email 邮箱
     */

    public void sendVCode(String email) {
        // 一个过期周期最多重发十条，记录已发送的邮箱以及间隔时间
        final int timeout = expire / 10;
        if (!redisCache.setCacheIfAbsent("HasBeenSentVCode:" + email , timeout, timeout)) {
            // 设置失败，说明 key 已存在
            throw new MaaResultException(403, String.format("发送验证码的请求至少需要间隔 %d 秒", timeout));
        }
        // 调用注入的代理类执行异步任务
        emailService.asyncSendVCode(email);
    }

    @Async
    protected void asyncSendVCode(String email) {
        // 6位随机数验证码
        String vcode = RandomStringUtils.random(6, true, true).toUpperCase();
        if (flagNoSend) {
            log.debug("vcode is " + vcode);
            log.warn("Email not sent, no-send enabled");
        } else {
            EmailBusinessObject.builder()
                    .setMailAccount(MAIL_ACCOUNT)
                    .setEmail(email)
                    .sendVerificationCodeMessage(vcode);
        }
        // 存redis
        redisCache.setCache("vCodeEmail:" + email, vcode, expire);
    }

    /**
     * 检验验证码并抛出异常
     * @param email 邮箱
     * @param vcode 验证码
     * @throws MaaResultException 验证码错误
     */
    public void verifyVCode(String email, String vcode) {
        if (!redisCache.removeKVIfEquals("vCodeEmail:" + email, vcode.toUpperCase())) {
            throw new MaaResultException(401, "验证码错误");
        }
    }

    @Async
    public void sendCommentNotification(String email, CommentNotification commentNotification) {
        int limit = 25;

        String title = commentNotification.getTitle();
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
                .setTitle("收到新回复 来自用户@" + commentNotification.getReName() + " Re: " + map.get("title"))
                .setMailAccount(MAIL_ACCOUNT)
                .setEmail(email)
                .sendCommentNotification(map);

    }
}
