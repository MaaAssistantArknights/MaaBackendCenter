package plus.maa.backend.controller.response.user

import java.time.Instant

data class MaaLoginRsp(
    val token: String,
    val validBefore: Instant,
    val validAfter: Instant,
    val refreshToken: String,
    val refreshTokenValidBefore: Instant,
    val refreshTokenValidAfter: Instant,
    val userInfo: MaaUserInfo
)
