package plus.maa.backend.service;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.service.model.LoginUser;

@Service
public class UserSessionService {

    private static final String REDIS_KEY_PREFIX_LOGIN = "LOGIN:";

    private static String buildUserCacheKey(String userId) {
        return REDIS_KEY_PREFIX_LOGIN + userId;
    }

    private final RedisCache cache;

    public UserSessionService(RedisCache cache) {
        this.cache = cache;
    }

    @Nullable
    public LoginUser getUser(String id) {
        return cache.getCache(buildUserCacheKey(id), LoginUser.class);
    }

    public void setUser(@NotNull LoginUser user) {
        cache.setCache(buildUserCacheKey(user.getUserId()), user);
    }

    public void removeUser(String id) {
        cache.removeCache(buildUserCacheKey(id));
    }

}
