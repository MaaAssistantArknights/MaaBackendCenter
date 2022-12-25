package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.bo.EmailBusinessObject;
import plus.maa.backend.repository.RedisCache;

import java.util.concurrent.TimeUnit;

/**
 * @author LoMu
 * Date  2022-12-24 11:05
 */
@Service
@RequiredArgsConstructor
public class EmailService {
    private final RedisCache redisCache;
    @Value("${maa-copilot.vcode.expire:600}")
    private int expire;

    /**
     * 发送验证码
     * 以email作为 redis key
     * vcode(验证码)作为 redis value
     *
     * @param email 邮箱
     */
    public void sendCaptcha(String email) {
        //6位随机数验证码
        String captcha = RandomStringUtils.random(6, true, true);
        new EmailBusinessObject()
                .setEmail(email)
                .sendVerificationCodeMessage(captcha);
        //存redis
        redisCache.setCacheEmailVerificationCode("vCodeEmail:" + email, captcha, expire, TimeUnit.SECONDS);

    }
}
