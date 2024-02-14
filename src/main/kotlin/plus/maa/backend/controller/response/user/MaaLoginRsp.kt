package plus.maa.backend.controller.response.user

import java.time.LocalDateTime

data class MaaLoginRsp(
    val token: String,
    val validBefore: LocalDateTime,
    val validAfter: LocalDateTime,
    val refreshToken: String,
    val refreshTokenValidBefore: LocalDateTime,
    val refreshTokenValidAfter: LocalDateTime,
    val userInfo: MaaUserInfo
)
