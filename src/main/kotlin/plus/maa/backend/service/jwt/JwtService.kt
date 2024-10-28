package plus.maa.backend.service.jwt

import org.springframework.security.core.GrantedAuthority
import org.springframework.stereotype.Service
import plus.maa.backend.config.external.MaaCopilotProperties
import java.time.Instant

/**
 * 基于 Jwt 的 token 服务。 可直接用于 stateless 情境下的签发和认证， 或结合数据库进行状态管理。
 * 建议 AuthToken 使用无状态方案, RefreshToken 使用有状态方案
 */
@Service
class JwtService(properties: MaaCopilotProperties) {
    private val jwtProperties = properties.jwt
    private val key = jwtProperties.secret.toByteArray()

    /**
     * 签发 AuthToken. 过期时间由配置的 jwt.expire 计算而来
     *
     * @param subject     签发对象，一般设置为对象的标识符
     * @param jwtId       jwt 的 id， 一般用于 stateful 场景下
     * @param authorities 授予的权限
     * @return JwtAuthToken
     */
    fun issueAuthToken(subject: String, jwtId: String?, authorities: Collection<GrantedAuthority>): JwtAuthToken {
        val now = Instant.now()
        val expireAt = now.plusSeconds(jwtProperties.expire)
        return JwtAuthToken(subject, jwtId, now, expireAt, now, authorities, key)
    }

    /**
     * 验证并解析为 AuthToken. 该方法为 stateless 的验证。
     *
     * @param authToken jwt 字符串
     * @return JwtAuthToken
     * @throws JwtInvalidException jwt不符合要求
     * @throws JwtExpiredException jwt未生效或者已过期
     */
    @Throws(JwtInvalidException::class, JwtExpiredException::class)
    fun verifyAndParseAuthToken(authToken: String): JwtAuthToken {
        val token = JwtAuthToken(authToken, key)
        token.validateDate(Instant.now())
        token.isAuthenticated = true
        return token
    }

    /**
     * 签发 RefreshToken. 过期时间由配置的 Jwt.getRefreshExpire 计算而来
     *
     * @param subject 签发对象，一般设置为对象的标识符
     * @param jwtId   jwt 的 id， 一般用于 stateful 场景下
     * @return JwtAuthToken
     */
    fun issueRefreshToken(subject: String, jwtId: String?): JwtRefreshToken {
        val now = Instant.now()
        val expireAt = now.plusSeconds(jwtProperties.refreshExpire)
        return JwtRefreshToken(subject, jwtId, now, expireAt, now, key)
    }

    /**
     * 验证并解析为 RefreshToken. 该方法为 stateless 的验证。
     *
     * @param refreshToken jwt字符串
     * @return RefreshToken
     * @throws JwtInvalidException jwt不符合要求
     * @throws JwtExpiredException jwt未生效或者已过期
     */
    @Throws(JwtInvalidException::class, JwtExpiredException::class)
    fun verifyAndParseRefreshToken(refreshToken: String): JwtRefreshToken {
        val token = JwtRefreshToken(refreshToken, key)
        token.validateDate(Instant.now())
        return token
    }
}
