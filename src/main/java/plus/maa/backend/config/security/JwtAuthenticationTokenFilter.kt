package plus.maa.backend.config.security

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.service.jwt.JwtService
import java.io.IOException

/**
 * @author AnselYuki
 */
@Component
class JwtAuthenticationTokenFilter(
    private val helper: AuthenticationHelper,
    private val properties: MaaCopilotProperties,
    private val jwtService: JwtService
) : OncePerRequestFilter() {
    @Throws(IOException::class, ServletException::class)
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val token = extractToken(request)
            val authToken = jwtService.verifyAndParseAuthToken(token)
            helper.setAuthentication(authToken)
        } catch (ex: Exception) {
            logger.trace(ex.message)
        } finally {
            filterChain.doFilter(request, response)
        }
    }

    @Throws(Exception::class)
    private fun extractToken(request: HttpServletRequest): String {
        if (SecurityContextHolder.getContext().authentication != null) throw Exception("no need to auth")
        val head = request.getHeader(properties.jwt.header)
        if (head == null || !head.startsWith("Bearer ")) throw Exception("token not found")
        return head.substring(7)
    }
}
