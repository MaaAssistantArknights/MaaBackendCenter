package plus.maa.backend.config.security

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import plus.maa.backend.common.utils.WebUtils.renderString
import plus.maa.backend.controller.response.MaaResult.Companion.fail
import java.io.IOException

/**
 * @author AnselYuki
 */
@Component
class AccessDeniedHandlerImpl : AccessDeniedHandler {
    @Throws(IOException::class)
    override fun handle(request: HttpServletRequest, response: HttpServletResponse, accessDeniedException: AccessDeniedException) {
        val result = fail(HttpStatus.FORBIDDEN.value(), "权限不足")
        val json = ObjectMapper().writeValueAsString(result)
        renderString(response, json, HttpStatus.FORBIDDEN.value())
    }
}
