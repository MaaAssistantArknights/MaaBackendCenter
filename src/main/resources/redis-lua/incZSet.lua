local member = ARGV[1]
local incScore = ARGV[2]
local size = tonumber(ARGV[3])
local timeout = tonumber(ARGV[4])
redis.call('ZINCRBY', KEYS[1], incScore, member)
if timeout > 0 then -- timeout 必须是数字
    redis.call('EXPIRE', KEYS[1], timeout)
end
local count = redis.call('ZCARD', KEYS[1])
if count > size + 50 then   -- 这里 size 就算不转数字也能用，是因为部分运算会自动转换字符串
    redis.call('ZREMRANGEBYRANK', KEYS[1], 0, count - size - 1)
end