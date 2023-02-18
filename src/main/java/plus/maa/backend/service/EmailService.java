package plus.maa.backend.service;

import java.util.UUID;

import cn.hutool.extra.mail.MailAccount;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import plus.maa.backend.common.bo.EmailBusinessObject;
import plus.maa.backend.repository.RedisCache;

/**
 * @author LoMu
 *         Date 2022-12-24 11:05
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {
    @Value("${maa-copilot.vcode.expire:600}")
    private int expire;

    @Value("${maa-copilot.info.domain}")
    private String domain;

    @Value("${maa-copilot.mail.host}")
    private String host;
    @Value("${maa-copilot.mail.port}")
    private Integer port;
    @Value("${maa-copilot.mail.from}")
    private String from;
    @Value("${maa-copilot.mail.user}")
    private String user;
    @Value("${maa-copilot.mail.pass}")
    private String pass;
    @Value("${maa-copilot.mail.starttls}")
    private boolean starttls;
    @Value("${maa-copilot.mail.ssl}")
    private boolean ssl;
    @Value("${debug.email.no-send:false}")
    private boolean flagNoSend;

    private final RedisCache redisCache;

    /**
     * 装配发件人信息
     *
     * @return mailAccount
     */
    private MailAccount getMailAccount() {
        MailAccount mailAccount = new MailAccount();
        mailAccount
                .setHost(host)
                .setPort(port)
                .setFrom(from)
                .setUser(user)
                .setPass(pass)
                .setSslEnable(ssl)
                .setStarttlsEnable(starttls);
        return mailAccount;
    }

    /**
     * 发送验证码
     * 以email作为 redis key
     * vcode(验证码)作为 redis value
     *
     * @param email 邮箱
     */
    public void sendVCode(String email) {
        // 6位随机数验证码
        String vcode = RandomStringUtils.random(6, true, true).toUpperCase();
        if (flagNoSend) {
            log.debug("vcode is " + vcode);
            log.warn("Email not sent, no-send enabled");
        } else {
            EmailBusinessObject.builder()
                    .setMailAccount(getMailAccount())
                    .setEmail(email)
                    .sendVerificationCodeMessage(vcode);
        }
        // 存redis
        redisCache.setCache("vCodeEmail:" + email, vcode, expire);
    }

    public void verifyVCode(String email, String vcode) {
        String cacheVCode = redisCache.getCache("vCodeEmail:" + email, String.class);
        Assert.state(StringUtils.equalsIgnoreCase(cacheVCode, vcode), "验证码错误");
        redisCache.removeCache("vCodeEmail:" + email);
    }

    /**
     * 验证发到某个邮箱的验证码
     * @param email 邮箱
     * @param vcode 验证码
     * @param clearVCodeOnSuccess 验证成功是否删除验证码
     * @return 是否一致
     */
    public boolean verifyVCode2(String email, String vcode,boolean clearVCodeOnSuccess) {
        // FIXME:可能出现多线程数据争用问题，想办法用redis的一些方法直接比较完删除
        String cacheVCode = redisCache.getCache("vCodeEmail:" + email, String.class);
        Boolean result= StringUtils.equalsIgnoreCase(cacheVCode, vcode);
        if (clearVCodeOnSuccess && result) {
            redisCache.removeCache("vCodeEmail:" + email);
        }
        return result;
    }

    /**
     * @param email 发送激活验证邮箱
     */
    public void sendActivateUrl(String email) {
        // 生成uuid作为唯一标识符
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String url = domain + "/user/activateAccount?nonce=" + uuid;
        if (flagNoSend) {
            log.debug("url is " + url);
            log.warn("Email not sent, no-send enabled");
        } else {
            EmailBusinessObject.builder()
                    .setMailAccount(getMailAccount())
                    .setEmail(email)
                    .sendActivateUrlMessage(url);
        }
        // 存redis
        redisCache.setCache("UUID:" + uuid, email, expire);
    }

}
