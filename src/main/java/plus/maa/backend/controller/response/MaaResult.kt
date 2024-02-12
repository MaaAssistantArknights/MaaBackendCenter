package plus.maa.backend.controller.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * @author AnselYuki
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
sealed interface MaaResult<out T> {
    data class Success<T>(val statusCode: Int, val message: String?, val data: T) : MaaResult<T>
    data class Fail(val statusCode: Int, val message: String?) : MaaResult<Nothing>
    companion object {
        fun <T> success(data: T): Success<T> {
            return success(null, data)
        }

        fun success(): Success<Unit> {
            return success(null, Unit)
        }

        fun <T> success(msg: String?, data: T): Success<T> {
            return Success(200, msg, data)
        }

        fun fail(code: Int, msg: String?): Fail {
            return Fail(code, msg)
        }

    }
}