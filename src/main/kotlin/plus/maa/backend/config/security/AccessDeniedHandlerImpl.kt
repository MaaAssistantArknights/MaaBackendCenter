package plus.maa.backend.config.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import plus.maa.backend.controller.response.MaaResult.Companion.fail
import plus.maa.backend.service.DataTransferService
import java.io.IOException

/**
 * @author AnselYuki
 */
@Component
class AccessDeniedHandlerImpl(private val dataTransferService: DataTransferService) : AccessDeniedHandler {
    @Throws(IOException::class)
    override fun handle(request: HttpServletRequest, response: HttpServletResponse, accessDeniedException: AccessDeniedException) {
        val result = fail(HttpStatus.FORBIDDEN.value(), "权限不足")
        dataTransferService.writeJson(response, result, HttpStatus.FORBIDDEN.value())
    }
}
