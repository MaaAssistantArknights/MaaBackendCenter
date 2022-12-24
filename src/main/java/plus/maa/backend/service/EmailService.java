package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.utils.EmailUtils;
import plus.maa.backend.repository.RedisCache;

import java.util.concurrent.TimeUnit;

/**
 * @author LoMu
 * Date  2022-12-24 11:05
 */
@Service
@RequiredArgsConstructor
public class EmailService {
    @Value("${maa-copilot.vcode.expire}")
    private int expire;

    private final RedisCache redisCache;

    /**
     * 发送验证码
     * 以email作为 redis key
     * vcode(验证码)作为 redis value
     * @param email 邮箱
     * @param vcode 验证码
     */
    public void sendVCode(String email,String vcode){
        new EmailUtils()
                .setEmail(email)
                .sendVerificationCodeMessage(vcode);
        //存redis 默认10分钟失效
        redisCache.setCacheEmailVerificationCode(email,vcode,expire, TimeUnit.SECONDS);

    }
}
