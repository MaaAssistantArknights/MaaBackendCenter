package plus.maa.backend.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Redis工具类
 *
 * @author AnselYuki
 */
@Slf4j
@Setter
@Component
@RequiredArgsConstructor
public class RedisCache {
    @Value("${maa-copilot.cache.default-expire}")
    private int expire;

    private final StringRedisTemplate redisTemplate;

    //  添加 JSR310 模块，以便顺利序列化 LocalDateTime 等类型
    private final ObjectMapper writeMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private final ObjectMapper readMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

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
            json = writeMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage(), e);
            }
            return;
        }
        if (timeout <= 0) {
            redisTemplate.opsForValue().set(key, json);
        } else {
            redisTemplate.opsForValue().set(key, json, timeout, timeUnit);
        }
    }

    public <T> void setTheSet(final String key, Collection<T> set, long timeout) {
        setTheSet(key, set, timeout, TimeUnit.SECONDS);
    }

    public <T> void setTheSet(final String key, Collection<T> set, long timeout, TimeUnit timeUnit) {
        String[] jsonList = new String[set.size()];
        try {
            int i = 0;
            for (T t : set) {
                jsonList[i++] = writeMapper.writeValueAsString(t);
            }
        } catch (JsonProcessingException e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage(), e);
            }
            return;
        }
        if (timeout <= 0) {
            redisTemplate.opsForSet().add(key, jsonList);
        } else {
            redisTemplate.opsForSet().add(key, jsonList);
            redisTemplate.expire(key, timeout, timeUnit);
        }
    }

    public <T> boolean valueMemberInSet(final String key, T value) {
        String json;
        try {
            json = writeMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage(), e);
            }
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, json));
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
            result = readMapper.readValue(json, valueType);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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
                    result = readMapper.readValue(json, valueType);
                }
                result = onUpdate.apply(result);
                setCache(key, result, timeout, timeUnit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
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

    /**
     * 模糊删除缓存。
     *
     * @param pattern 待删除的 Key 表达式，例如 "home:*" 表示删除 Key 以 "home:" 开头的所有缓存
     * @author Lixuhuilll
     */
    public void removeCacheByPattern(String pattern) {
        // 批量删除的阈值
        final int batchSize = 10000;
        // 构造 ScanOptions
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .count(batchSize)
                .match(pattern)
                .build();

        // 保存要删除的键
        List<String> keysToDelete = new ArrayList<>();

        // try-with-resources 自动关闭 SCAN
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                // 将要删除的键添加到列表中
                keysToDelete.add(key);

                // 如果达到批量删除的阈值，则执行批量删除
                if (keysToDelete.size() >= batchSize) {
                    redisTemplate.delete(keysToDelete);
                    keysToDelete.clear();
                }
            }
        }

        // 删除剩余的键（不足 batchSize 的最后一批）
        if (!keysToDelete.isEmpty()) {
            redisTemplate.delete(keysToDelete);
        }
    }
}
