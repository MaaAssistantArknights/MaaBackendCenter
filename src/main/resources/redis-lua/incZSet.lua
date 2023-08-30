local member = ARGV[1]
local incScore = ARGV[2]
local size = ARGV[3]
local timeout = ARGV[4]
redis.call('ZINCRBY', KEYS[1], incScore, member)
redis.call('EXPIRE', KEYS[1], timeout)
local count = redis.call('ZCARD', KEYS[1])
if count > size + 50 then
    redis.call('ZREMRANGEBYRANK', KEYS[1], 0, count - size - 1)
end