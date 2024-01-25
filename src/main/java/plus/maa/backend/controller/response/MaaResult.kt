package plus.maa.backend.controller.response

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * @author AnselYuki
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class MaaResult<T>(val statusCode: Int, val message: String?, val data: T) {
    companion object {
        @JvmStatic
        fun <T> success(data: T): MaaResult<T> {
            return success(null, data)
        }

        @JvmStatic
        fun <T> success(): MaaResult<T?> {
            return success(null, null)
        }

        @JvmStatic
        fun <T> success(msg: String?, data: T): MaaResult<T> {
            return MaaResult(200, msg, data)
        }

        @JvmStatic
        fun <T> fail(code: Int, msg: String?): MaaResult<T?> {
            return fail(code, msg, null)
        }

        @JvmStatic
        fun <T> fail(code: Int, msg: String?, data: T): MaaResult<T> {
            return MaaResult(code, msg, data)
        }
    }
}