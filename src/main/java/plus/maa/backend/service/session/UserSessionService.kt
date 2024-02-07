package plus.maa.backend.service.session

import org.springframework.stereotype.Service
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.repository.RedisCache

@Service
class UserSessionService(private val cache: RedisCache, properties: MaaCopilotProperties) {
    private val sessionExpiration = properties.jwt.refreshExpire

    fun getSession(id: String): UserSession? {
        return cache.getCache(buildUserCacheKey(id), UserSession::class.java, null, sessionExpiration)
    }

    fun setSession(id: String, session: UserSession) {
        cache.setCache(buildUserCacheKey(id), session, sessionExpiration)
    }

    fun setSession(id: String, consumer: (UserSession) -> Unit) {
        val session = UserSession()
        consumer(session)
        cache.setCache(buildUserCacheKey(id), session, sessionExpiration)
    }

    fun updateSessionIfPresent(id: String, consumer: (UserSession) -> Unit){
        cache.updateCache(id, UserSession::class.java, null, { session: UserSession? ->
            if (session != null) consumer(session)
            session
        }, sessionExpiration)
    }

    fun removeSession(id: String) {
        cache.removeCache(buildUserCacheKey(id))
    }

    companion object {
        private const val REDIS_USER_SESSION_PREFIX = "USER_SESSION_"

        private fun buildUserCacheKey(userId: String): String {
            return REDIS_USER_SESSION_PREFIX + userId
        }
    }
}
