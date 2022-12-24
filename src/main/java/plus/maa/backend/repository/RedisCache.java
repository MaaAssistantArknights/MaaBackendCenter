package plus.maa.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import plus.maa.backend.service.model.LoginUser;

import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 *
 * @author AnselYuki
 */
@Setter
@Component
@RequiredArgsConstructor
public class RedisCache {
    private final StringRedisTemplate redisTemplate;

    public void setCacheLoginUser(final String key, LoginUser value, long timeout, TimeUnit timeUnit) {
        String str;
        try {
            str = new ObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return;
        }
        redisTemplate.opsForValue().set(key, str, timeout, timeUnit);
    }

    public LoginUser getCacheLoginUser(final String key) {
        LoginUser loginUser;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                return null;
            }
            loginUser = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(json, LoginUser.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return loginUser;
    }

    /**
     * 验证码缓存 以邮箱为Key
     *
     * @param emailKey         vCodeEmail:邮箱形式传入
     * @param verificationCode 验证码
     * @param timeout          超时
     * @param timeUnit         时间类型
     */
    public void setCacheEmailVerificationCode(final String emailKey, final String verificationCode, long timeout, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(emailKey, verificationCode, timeout, timeUnit);
    }

    /**
     * 获取缓存信息
     *
     * @param emailKey         key
     * @param verificationCode code
     * @return boolean
     */
    public boolean checkCacheEmailVerificationCode(final String emailKey, final String verificationCode) {
        if (!emailKey.contains("vCodeEmail:")) {
            //这个不可能抛给前端吧？
            throw new RuntimeException("获取缓存Key类型不匹配,需以[vCodeEmail:邮箱]形式传入Key");
        }
        String vCode = redisTemplate.opsForValue().get(emailKey);
        if (!"".equals(vCode) && verificationCode.equals(vCode)) return true;
        else return false;
    }
}