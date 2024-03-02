package plus.maa.backend.controller.response

import org.springframework.http.HttpStatus
import plus.maa.backend.common.MaaStatusCode

/**
 * @author john180
 */
class MaaResultException(
    val code: Int,
    val msg: String?
) : RuntimeException() {

    constructor(statusCode: MaaStatusCode) : this(statusCode.code, statusCode.message)
    constructor(msg: String) : this(HttpStatus.INTERNAL_SERVER_ERROR.value(), msg)

}
