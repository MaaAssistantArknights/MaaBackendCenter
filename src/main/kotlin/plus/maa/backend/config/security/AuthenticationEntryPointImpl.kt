package plus.maa.backend.config.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import plus.maa.backend.controller.response.MaaResult.Companion.fail
import plus.maa.backend.service.DataTransferService
import java.io.IOException

/**
 * @author AnselYuki
 */
@Component
class AuthenticationEntryPointImpl(
    private val dataTransferService: DataTransferService,
) : AuthenticationEntryPoint {
    @Throws(IOException::class)
    override fun commence(request: HttpServletRequest, response: HttpServletResponse, authException: AuthenticationException) {
        val result = fail(HttpStatus.UNAUTHORIZED.value(), authException.message)
        dataTransferService.writeJson(response, result, HttpStatus.UNAUTHORIZED.value())
    }
}
