package plus.maa.backend.repository;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
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

    private final ObjectMapper oldObjectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();

    private final ObjectMapper objectMapper = JsonMapper.builder()
            // 序列化添加类型信息，并信任 Redis 中内容的反序列化
            .activateDefaultTyping(LaissezFaireSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
            // 适配 Spring Security 权限验证中用到的类
            .addModules(SecurityJackson2Modules.getModules(null))
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build();


    private final AtomicBoolean supportUnlink = new AtomicBoolean(true);

    /*
        使用 lua 脚本插入数据，维持 ZSet 的相对大小（size <= 实际大小 <= size + 50）以及过期时间
        实际大小这么设计是为了避免频繁的 ZREMRANGEBYRANK 操作
     */
    private final RedisScript<Object> incZSetRedisScript = RedisScript.of(new ClassPathResource("redis-lua/incZSet.lua"));
    // 比较与输入的键值对是否相同，相同则删除
    private final RedisScript<Boolean> removeKVIfEqualsScript = RedisScript.of(new ClassPathResource("redis-lua/removeKVIfEquals.lua"), Boolean.class);

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
        String json = getJson(value);
        if (json == null) return;
        if (timeout <= 0) {
            redisTemplate.opsForValue().set(key, json);
        } else {
            redisTemplate.opsForValue().set(key, json, timeout, timeUnit);
        }
    }

    /**
     * 当缓存不存在时，则 set
     *
     * @param key 缓存的 key
     * @param value 被缓存的值
     * @return  是否 set
     */

    public <T> boolean setCacheIfAbsent(final String key, T value) {
        return setCacheIfAbsent(key, value, expire);
    }

    /**
     * 当缓存不存在时，则 set
     *
     * @param key 缓存的 key
     * @param value 被缓存的值
     * @param timeout 过期时间
     * @return  是否 set
     */

    public <T> boolean setCacheIfAbsent(final String key, T value, long timeout) {
        return setCacheIfAbsent(key, value, timeout, TimeUnit.SECONDS);
    }

    /**
     * 当缓存不存在时，则 set
     *
     * @param key 缓存的 key
     * @param value 被缓存的值
     * @param timeout 过期时间
     * @param timeUnit 过期时间的单位
     * @return  是否 set
     */
    public <T> boolean setCacheIfAbsent(final String key, T value, long timeout, TimeUnit timeUnit) {
        String json = getJson(value);
        if (json == null) return false;
        boolean result;
        if (timeout <= 0) {
            result = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, json));
        } else {
            result = Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, json, timeout, timeUnit));
        }
        return result;
    }

    public <T> void addSet(final String key, Collection<T> set, long timeout) {
        addSet(key, set, timeout, TimeUnit.SECONDS);
    }

    public <T> void addSet(final String key, Collection<T> set, long timeout, TimeUnit timeUnit) {
        if (key == null || set == null || set.isEmpty()) { // Redis 会拒绝空集合
            return;
        }
        String[] jsonList = new String[set.size()];
        int i = 0;
        for (T t : set) {
            String json = getJson(t);
            if (json == null) return;
            jsonList[i++] = json;
        }

        if (timeout <= 0) {
            redisTemplate.opsForSet().add(key, jsonList);
        } else {
            redisTemplate.opsForSet().add(key, jsonList);
            redisTemplate.expire(key, timeout, timeUnit);
        }
    }


    /**
     * ZSet 中元素的 score += incScore，如果元素不存在则插入 <br>
     * 会维持 ZSet 的相对大小（size <= 实际大小 <= size + 50）以及过期时间 <br>
     * 当大小超出 size + 50 时，会优先删除 score 最小的元素，直到大小等于 size
     *
     * @param key      ZSet 的 key
     * @param member   ZSet 的 member
     * @param incScore 增加的 score
     * @param size     ZSet 的相对大小
     * @param timeout  ZSet 的过期时间
     */

    public void incZSet(final String key, String member, double incScore, long size, long timeout) {
        redisTemplate.execute(incZSetRedisScript, List.of(key), member, Double.toString(incScore), Long.toString(size), Long.toString(timeout));
    }

    // 获取的元素是按照 score 从小到大排列的
    @Nullable
    public Set<String> getZSet(final String key, long start, long end) {
        return redisTemplate.opsForZSet().range(key, start, end);
    }

    // 获取的元素是按照 score 从大到小排列的
    @Nullable
    public Set<String> getZSetReverse(final String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    public <T> boolean valueMemberInSet(final String key, T value) {
        try {
            String json = getJson(value);
            if (json == null) return false;
            return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, json));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return false;
    }

    @Nullable
    public <T> T getCache(final String key, Class<T> valueType) {
        return getCache(key, valueType, null, expire, TimeUnit.SECONDS);
    }

    @Nullable
    public <T> T getCache(final String key, Class<T> valueType, Supplier<T> onMiss) {
        return getCache(key, valueType, onMiss, expire, TimeUnit.SECONDS);
    }

    @Nullable
    public <T> T getCache(final String key, Class<T> valueType, Supplier<T> onMiss, long timeout) {
        return getCache(key, valueType, onMiss, timeout, TimeUnit.SECONDS);
    }

    @Nullable
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
            result = readJsonValue(json, valueType);
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
                    result = readJsonValue(json, valueType);
                }
                result = onUpdate.apply(result);
                setCache(key, result, timeout, timeUnit);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    @Nullable
    public String getCacheLevelCommit() {
        return getCache("level:commit", String.class);
    }

    public void setCacheLevelCommit(String commit) {
        setData("level:commit", commit);
    }

    public void removeCache(String key) {
        removeCache(key, false);
    }

    public void removeCache(String key, boolean notUseUnlink) {
        removeCache(List.of(key), notUseUnlink);
    }

    public void removeCache(Collection<String> keys) {
        removeCache(keys, false);
    }

    public void removeCache(Collection<String> keys, boolean notUseUnlink) {

        if (!notUseUnlink && supportUnlink.get()) {
            try {
                redisTemplate.unlink(keys);
                return;
            } catch (InvalidDataAccessApiUsageException | RedisSystemException e) {
                // Redisson、Jedis、Lettuce
                Throwable cause = e.getCause();
                if (cause == null || !StringUtils.containsAny(
                        cause.getMessage(), "unknown command", "not support")) {
                    throw e;
                }
                if (supportUnlink.compareAndSet(true, false)) {
                    log.warn("当前连接的 Redis Service 可能不支持 Unlink 命令，切换为 Del");
                }
            }
        }

        // 兜底的 Del 命令
        redisTemplate.delete(keys);
    }

    /**
     * 相同则删除键值对
     *
     * @param key 待比较和删除的键
     * @param value 待比较的值
     * @return 是否删除
     */
    public <T> boolean removeKVIfEquals(String key, T value) {
        String json = getJson(value);
        if (json == null) return false;
        return Boolean.TRUE.equals(
                redisTemplate.execute(removeKVIfEqualsScript, List.of(key), json)
        );
    }

    /**
     * 模糊删除缓存。不保证立即删除，不保证完全删除。<br>
     * 异步，因为 Scan 虽然不会阻塞 Redis，但客户端会阻塞
     *
     * @param pattern 待删除的 Key 表达式，例如 "home:*" 表示删除 Key 以 "home:" 开头的所有缓存
     * @author Lixuhuilll
     */
    @Async
    public void removeCacheByPattern(String pattern) {
        syncRemoveCacheByPattern(pattern);
    }

    /**
     * 模糊删除缓存。不保证立即删除，不保证完全删除。<br>
     * 同步调用 Scan，不会长时间阻塞 Redis，但会阻塞客户端，阻塞时间视 Redis 中 key 的数量而定。
     * 删除期间，其他线程或客户端可对 Redis 进行 CURD（因为不阻塞 Redis），因此不保证删除的时机，也不保证完全删除干净
     *
     * @param pattern 待删除的 Key 表达式，例如 "home:*" 表示删除 Key 以 "home:" 开头的所有缓存
     * @author Lixuhuilll
     */
    public void syncRemoveCacheByPattern(String pattern) {
        // 批量删除的阈值
        final int batchSize = 2000;
        // 构造 ScanOptions
        ScanOptions scanOptions = ScanOptions.scanOptions()
                .count(batchSize)
                .match(pattern)
                .build();

        // 保存要删除的键
        List<String> keysToDelete = new ArrayList<>(batchSize);

        // try-with-resources 自动关闭 SCAN
        try (Cursor<String> cursor = redisTemplate.scan(scanOptions)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                // 将要删除的键添加到列表中
                keysToDelete.add(key);

                // 如果达到批量删除的阈值，则执行批量删除
                if (keysToDelete.size() >= batchSize) {
                    removeCache(keysToDelete);
                    keysToDelete.clear();
                }
            }
        }

        // 删除剩余的键（不足 batchSize 的最后一批）
        if (!keysToDelete.isEmpty()) {
            removeCache(keysToDelete);
        }
    }


    @Nullable
    private <T> String getJson(T value) {
        String json;
        try {
            json = objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage(), e);
            }
            return null;
        }
        return json;
    }

    private <T> T readJsonValue(String json, Class<T> valueType) throws JsonProcessingException {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (InvalidTypeIdException e) {
            // 缺少类型信息时，回退到旧版本
            return oldObjectMapper.readValue(json, valueType);
        }
    }
}
