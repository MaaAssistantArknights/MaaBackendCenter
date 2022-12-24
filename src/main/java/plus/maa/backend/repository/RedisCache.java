package plus.maa.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

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
    interface CacheMissFunction<T> {
        T GetData();
    }

    @Value("${maa-copilot.cache.default-expire}")
    private int expire;

    private final StringRedisTemplate redisTemplate;

    public <T> void setCache(final String key, T value) {
        setCache(key, value, 0, TimeUnit.SECONDS);
    }

    public <T> void setCache(final String key, T value, long timeout) {
        setCache(key, value, timeout, TimeUnit.SECONDS);
    }

    public <T> void setCache(final String key, T value, long timeout, TimeUnit timeUnit) {
        String json;
        try {
            json = new ObjectMapper().writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return;
        }
        if (timeout <= 0) {
            redisTemplate.opsForValue().set(key, json);
        } else {
            redisTemplate.opsForValue().set(key, json, timeout, timeUnit);
        }
    }

    public <T> T getCache(final String key, Class<T> valueType) {
        return getCache(key, valueType, null, expire, TimeUnit.SECONDS);
    }

    public <T> T getCache(final String key, Class<T> valueType, CacheMissFunction<T> missFunction) {
        return getCache(key, valueType, missFunction, expire, TimeUnit.SECONDS);
    }

    public <T> T getCache(final String key, Class<T> valueType, CacheMissFunction<T> missFunction, long timeout) {
        return getCache(key, valueType, missFunction, timeout, TimeUnit.SECONDS);
    }

    public <T> T getCache(final String key, Class<T> valueType, CacheMissFunction<T> missFunction, long timeout, TimeUnit timeUnit) {
        T result;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isEmpty()) {
                if (missFunction != null) {
                    //上锁
                    synchronized (RedisCache.class) {
                        //再次查询缓存，目的是判断是否前面的线程已经set过了
                        json = redisTemplate.opsForValue().get(key);
                        //第二次校验缓存是否存在
                        if (json == null || json.isEmpty()) {
                            result = missFunction.GetData();
                            //数据库中不存在
                            if (result == null) {
                                return null;
                            }
                            setCache(key, result, timeout, timeUnit);
                            return result;
                        }
                    }
                } else {
                    return null;
                }
            }
            result = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(json, valueType);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return result;
    }

    public String getCacheLevelCommit() {
        return getCache("level:commit", String.class);
    }

    public void setCacheLevelCommit(String commit) {
        setCache("level:commit", commit);
    }
}