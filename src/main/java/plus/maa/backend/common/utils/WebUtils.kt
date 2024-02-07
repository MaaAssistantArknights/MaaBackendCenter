package plus.maa.backend.common.utils

import jakarta.servlet.http.HttpServletResponse
import java.io.IOException

/**
 * @author AnselYuki
 */
object WebUtils {
    fun renderString(response: HttpServletResponse, json: String?, code: Int) {
        try {
            response.status = code
            response.contentType = "application/json"
            response.characterEncoding = "UTF-8"
            response.writer.println(json)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
