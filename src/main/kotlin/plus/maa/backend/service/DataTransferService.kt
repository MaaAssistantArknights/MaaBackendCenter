package plus.maa.backend.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.util.MimeTypeUtils
import java.io.IOException

/**
 * @author AnselYuki
 */
@Service
class DataTransferService(private val objectMapper: ObjectMapper) {
    fun <T> writeJson(response: HttpServletResponse, value: T, code: Int = HttpStatus.OK.value()) {
        try {
            response.status = code
            response.contentType = MimeTypeUtils.APPLICATION_JSON_VALUE
            response.characterEncoding = "UTF-8"
            objectMapper.writeValue(response.outputStream, value)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
