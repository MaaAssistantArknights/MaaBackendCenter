package plus.maa.backend.service.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Service;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.repository.RedisCache;

import java.util.function.Consumer;

@Service
public class UserSessionService {

    private static final String REDIS_USER_SESSION_PREFIX = "USER_SESSION_";

    private static String buildUserCacheKey(String userId) {
        return REDIS_USER_SESSION_PREFIX + userId;
    }

    private final RedisCache cache;
    private final long sessionExpiration;

    public UserSessionService(RedisCache cache, MaaCopilotProperties properties) {
        this.cache = cache;
        sessionExpiration = properties.getJwt().getRefreshExpire();
    }

    @Nullable
    public UserSession getSession(String id) {
        return cache.getCache(buildUserCacheKey(id), UserSession.class, null, sessionExpiration);
    }

    public void setSession(@NotNull String id, @NotNull UserSession session) {
        cache.setCache(buildUserCacheKey(id), session, sessionExpiration);
    }

    public void setSession(@NotNull String id,@NotNull Consumer<UserSession> consumer){
        var session = new UserSession();
        consumer.accept(session);
        cache.setCache(buildUserCacheKey(id), session, sessionExpiration);
    }

    public void updateSessionIfPresent(@NotNull String id, @NotNull Consumer<UserSession> consumer) {
        cache.updateCache(id, UserSession.class, null, (session) -> {
            if (session != null) consumer.accept(session);
            return session;
        }, sessionExpiration);
    }

    public void removeSession(String id) {
        cache.removeCache(buildUserCacheKey(id));
    }

}
