package plus.maa.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Redis工具类
 *
 * @author AnselYuki
 */
@Setter
@Component
@RequiredArgsConstructor
public class RedisCache {
    @Value("${maa-copilot.cache.default-expire}")
    private int expire;

    private final StringRedisTemplate redisTemplate;

    public <T> void setData(final String key, T value) {
        setCache(key, value, 0, TimeUnit.SECONDS);
    }

    public <T> void setCache(final String key, T value) {
        setCache(key, value, expire, TimeUnit.SECONDS);
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

    public <T> T getCache(final String key, Class<T> valueType, Supplier<T> onMiss) {
        return getCache(key, valueType, onMiss, expire, TimeUnit.SECONDS);
    }

    public <T> T getCache(final String key, Class<T> valueType, Supplier<T> onMiss, long timeout) {
        return getCache(key, valueType, onMiss, timeout, TimeUnit.SECONDS);
    }

    public <T> T getCache(final String key, Class<T> valueType, Supplier<T> onMiss, long timeout, TimeUnit timeUnit) {
        T result;
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (StringUtils.isEmpty(json)) {
                if (onMiss != null) {
                    //上锁
                    synchronized (RedisCache.class) {
                        //再次查询缓存，目的是判断是否前面的线程已经set过了
                        json = redisTemplate.opsForValue().get(key);
                        //第二次校验缓存是否存在
                        if (StringUtils.isEmpty(json)) {
                            result = onMiss.get();
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

    public <T> void updateCache(final String key, Class<T> valueType, T defaultValue, Function<T, T> onUpdate) {
        updateCache(key, valueType, defaultValue, onUpdate, expire, TimeUnit.SECONDS);
    }

    public <T> void updateCache(final String key, Class<T> valueType, T defaultValue, Function<T, T> onUpdate, long timeout) {
        updateCache(key, valueType, defaultValue, onUpdate, timeout, TimeUnit.SECONDS);
    }

    public <T> void updateCache(final String key, Class<T> valueType, T defaultValue, Function<T, T> onUpdate, long timeout, TimeUnit timeUnit) {
        T result;
        try {
            synchronized (RedisCache.class) {
                String json = redisTemplate.opsForValue().get(key);
                if (StringUtils.isEmpty(json)) {
                    result = defaultValue;
                } else {
                    result = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).readValue(json, valueType);
                }
                result = onUpdate.apply(result);
                setCache(key, result, timeout, timeUnit);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getCacheLevelCommit() {
        return getCache("level:commit", String.class);
    }

    public void setCacheLevelCommit(String commit) {
        setData("level:commit", commit);
    }

    public void removeCache(String key) {
        redisTemplate.delete(key);
    }
}
