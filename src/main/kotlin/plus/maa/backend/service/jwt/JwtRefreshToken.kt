package plus.maa.backend.service.jwt

import java.time.LocalDateTime

class JwtRefreshToken : JwtToken {
    /**
     * 从 jwt 构建 token
     *
     * @param token jwt
     * @param key   签名密钥
     * @throws JwtInvalidException jwt 未通过签名验证或不符合要求
     */
    constructor(token: String?, key: ByteArray?) : super(token, TYPE, key)

    constructor(
        sub: String,
        jti: String?,
        iat: LocalDateTime,
        exp: LocalDateTime,
        nbf: LocalDateTime,
        key: ByteArray
    ) : super(sub, jti, iat, exp, nbf, TYPE, key)

    companion object {
        /**
         * RefreshToken 类型值
         */
        const val TYPE: String = "refresh"
    }
}
