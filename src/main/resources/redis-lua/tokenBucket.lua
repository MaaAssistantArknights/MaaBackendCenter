-- 令牌桶 key
local key = KEYS[1]
-- 令牌桶最大容量
local max_tokens = tonumber(ARGV[1])
-- 令牌桶每秒填充量
local refill_rate = tonumber(ARGV[2])
-- 当前时间戳（单位：毫秒）
local current_timestamp = tonumber(ARGV[3])

if max_tokens <= 0 or refill_rate <= 0 then
    return false
end

-- 获取上次填充的时间戳（单位：毫秒）
local last_refill_time = tonumber(redis.call('HGET', key, 'last_refill_time') or 0)

-- 计算令牌量
local current_tokens
-- 判断是否为初始化访问
if last_refill_time == 0 then
    -- 令牌桶初始化为满
    current_tokens = max_tokens
else
    -- 计算自从上次填充以来生成的不考虑上限的令牌数量
    local no_limit_tokens = (current_timestamp - last_refill_time) / 1000.0 * refill_rate

    -- 获取当前的令牌量
    current_tokens = tonumber(redis.call('HGET', key, 'tokens') or 0)
    -- 计算新的当前令牌量（至少为 0，防止溢出或者时钟回拨）
    current_tokens = math.min(current_tokens + no_limit_tokens, max_tokens)
    current_tokens = math.max(0, current_tokens)
end

-- 更新令牌桶填入时间
redis.call('HSET', key, 'last_refill_time', current_timestamp)

-- 令牌桶视为从空桶开始，如果直到填满都未被使用，自动过期
redis.call('EXPIRE', key, math.ceil(max_tokens / refill_rate))

-- 如果可以消费一个令牌返回true，否则返回false
if current_tokens >= 1 then
    redis.call('HSET', key, 'tokens', current_tokens - 1)
    return true
else
    redis.call('HSET', key, 'tokens', current_tokens)
    return false
end