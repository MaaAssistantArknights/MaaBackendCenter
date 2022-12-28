package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.bo.EmailBusinessObject;
import plus.maa.backend.repository.RedisCache;

import java.util.Objects;

/**
 * @author LoMu
 * Date  2022-12-24 11:05
 */
@Service
@RequiredArgsConstructor
public class EmailService {
    @Value("${maa-copilot.vcode.expire:600}")
    private int expire;

    private final RedisCache redisCache;

    /**
     * 发送验证码
     * 以email作为 redis key
     * vcode(验证码)作为 redis value
     *
     * @param email 邮箱
     */
    public void sendVCode(String email) {
        //6位随机数验证码
        String vcode = RandomStringUtils.random(6, true, true);
        EmailBusinessObject.Builder()
                .setEmail(email)
                .sendVerificationCodeMessage(vcode);
        //存redis
        redisCache.setCache("vCodeEmail:" + email, vcode, expire);
    }

    public boolean verifyVCode(String email, String vcode) {
        String cacheVCode = redisCache.getCache("vCodeEmail:" + email, String.class);
        if (!Objects.equals(cacheVCode, vcode)) {
            throw new RuntimeException("验证码错误！");
        }
        return true;
    }
}
