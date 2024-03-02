package plus.maa.backend.config.security

import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import org.springframework.web.server.ResponseStatusException
import plus.maa.backend.common.utils.IpUtil.getIpAddr
import plus.maa.backend.service.jwt.JwtAuthToken
import plus.maa.backend.service.model.LoginUser
import java.util.*

/**
 * Auth 助手，统一 auth 的设置和获取
 */
@Component
class AuthenticationHelper {
    /**
     * 设置当前 auth， 是 SecurityContextHolder.getContext().setAuthentication(authentication) 的集中调用
     *
     * @param authentication 当前的 auth
     */
    fun setAuthentication(authentication: Authentication?) {
        SecurityContextHolder.getContext().authentication = authentication
    }

    /**
     * 要求用户 id ，否则抛出异常
     *
     * @return 已经验证的用户 id
     * @throws ResponseStatusException 用户未通过验证
     */
    @Throws(ResponseStatusException::class)
    fun requireUserId(): String {
        val id = userId ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED)
        return id
    }

    val userId: String?
        /**
         * 获取用户 id
         *
         * @return 用户 id，如未验证则返回 null
         */
        get() {
            val auth = SecurityContextHolder.getContext().authentication ?: return null
            if (auth is UsernamePasswordAuthenticationToken) {
                val principal = auth.getPrincipal()
                if (principal is LoginUser) return principal.userId
            } else if (auth is JwtAuthToken) {
                return auth.subject
            }
            return null
        }

    val userIdOrIpAddress: String
        /**
         * 获取已验证用户 id 或者未验证用户 ip 地址。在 HTTP request 之外调用该方法获取 ip 会抛出 NPE
         *
         * @return 用户 id 或者 ip 地址
         */
        get() {
            val id = userId
            if (id != null) return id

            val attributes = Objects.requireNonNull(RequestContextHolder.getRequestAttributes())
            val request = (attributes as ServletRequestAttributes).request
            return getIpAddr(request)
        }
}
