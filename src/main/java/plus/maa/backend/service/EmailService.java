package plus.maa.backend.service;

import java.util.UUID;

import cn.hutool.extra.mail.MailAccount;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import lombok.RequiredArgsConstructor;
import plus.maa.backend.common.bo.EmailBusinessObject;
import plus.maa.backend.repository.RedisCache;

/**
 * @author LoMu
 * Date  2022-12-24 11:05
 */
@Service
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
        //6位随机数验证码
        String vcode = RandomStringUtils.random(6, true, true).toUpperCase();
        EmailBusinessObject.builder()
                .setMailAccount(getMailAccount())
                .setEmail(email)
                .sendVerificationCodeMessage(vcode);
        //存redis
        redisCache.setCache("vCodeEmail:" + email, vcode, expire);
    }

    public void verifyVCode(String email, String vcode) {
        String cacheVCode = redisCache.getCache("vCodeEmail:" + email, String.class);
        Assert.state(StringUtils.equalsIgnoreCase(cacheVCode, vcode), "验证码错误");
        redisCache.removeCache("vCodeEmail:" + email);
    }


    /**
     * @param email 发送激活验证邮箱
     */
    public void sendActivateUrl(String email) {
        //生成uuid作为唯一标识符
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String url = domain + "/user/activateAccount?nonce=" + uuid;
        EmailBusinessObject.builder()
                .setMailAccount(getMailAccount())
                .setEmail(email)
                .sendActivateUrlMessage(url);
        //存redis
        redisCache.setCache("UUID:" + uuid, email, expire);
    }

}
