package plus.maa.backend.config.security

import io.mockk.every
import io.mockk.spyk
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.junit.jupiter.api.Test
import org.springframework.security.core.context.SecurityContextHolder
import plus.maa.backend.config.external.Jwt
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.service.jwt.JwtService

class JwtAuthenticationTokenFilterTest {
    @Test
    fun testValidToken() {
        val properties = MaaCopilotProperties()
        val jwtSettings = Jwt()
        jwtSettings.secret = "whatever you want"
        jwtSettings.expire = 86400
        properties.jwt = jwtSettings

        val jwtService = JwtService(properties)
        val userId = "some user id"
        val authToken = jwtService.issueAuthToken(userId, null, ArrayList())
        val jwt = authToken.value

        val filter = JwtAuthenticationTokenFilter(AuthenticationHelper(), properties, jwtService)
        val request = spyk<HttpServletRequest>()
        every { request.getHeader(properties.jwt.header) } returns "Bearer $jwt"
        val filterChain = spyk<FilterChain>()
        try {
            filter.doFilter(request, spyk<HttpServletResponse>(), filterChain)
        } catch (ignored: Exception) {
        }
        requireNotNull(SecurityContextHolder.getContext().authentication)
    }
}
