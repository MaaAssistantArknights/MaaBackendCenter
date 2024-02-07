package plus.maa.backend.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import lombok.RequiredArgsConstructor
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component
import plus.maa.backend.common.utils.WebUtils.renderString
import plus.maa.backend.controller.response.MaaResult.Companion.fail
import java.io.IOException

/**
 * @author AnselYuki
 */
@Component
@RequiredArgsConstructor
class AuthenticationEntryPointImpl : AuthenticationEntryPoint {
    private val objectMapper: ObjectMapper? = null

    @Throws(IOException::class)
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        val result = fail<Void>(HttpStatus.UNAUTHORIZED.value(), authException.message)
        val json = objectMapper!!.writeValueAsString(result)
        renderString(response, json, HttpStatus.UNAUTHORIZED.value())
    }
}
