local member = ARGV[1]
local incScore = ARGV[2]
local size = tonumber(ARGV[3])
local timeout = tonumber(ARGV[4])
redis.call('ZINCRBY', KEYS[1], incScore, member)
-- 判断是否应该缩小 ZSet，避免频繁操作过于浪费时间
local count = redis.call('ZCARD', KEYS[1])
if count > size * 1.5 then
    redis.call('ZREMRANGEBYRANK', KEYS[1], 0, count - size - 1)
end
-- 配置过期时间应该在所有写入操作后执行，防止极小概率下出现永久 key
if timeout > 0 then
    redis.call('EXPIRE', KEYS[1], timeout)
end
