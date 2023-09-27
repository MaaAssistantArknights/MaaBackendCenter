local dbValue = redis.call('GET', KEYS[1])
if dbValue == ARGV[1] then
    redis.call('DEL', KEYS[1])
    return true
end
return false