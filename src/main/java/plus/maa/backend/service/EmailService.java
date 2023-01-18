package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.bo.EmailBusinessObject;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.repository.RedisCache;

import java.util.Objects;
import java.util.UUID;

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
        String vcode = RandomStringUtils.random(6, true, true).toUpperCase();
        EmailBusinessObject.builder()
                .setEmail(email)
                .sendVerificationCodeMessage(vcode);
        //存redis
        redisCache.setCache("vCodeEmail:" + email, vcode, expire);
    }

    public boolean verifyVCode(String email, String vcode) {
        String cacheVCode = redisCache.getCache("vCodeEmail:" + email, String.class);
        if (!Objects.equals(cacheVCode, vcode.toUpperCase())) {
            throw new MaaResultException("验证码错误！");
        }
        redisCache.removeCache("vCodeEmail:" + email);
        return true;
    }


    /**
     * @param email 发送激活验证邮箱
     */
    public void sendActivateUrl(String email) {
        //生成uuid作为唯一标识符
        String uuid = UUID.randomUUID().toString().replaceAll("-", "");
        String url = domain + "/user/activateAccount?nonce=" + uuid;
        EmailBusinessObject.builder()
                .setEmail(email)
                .sendActivateUrlMessage(url);
        //存redis
        redisCache.setCache("UUID:" + uuid, email, expire);
    }

}
