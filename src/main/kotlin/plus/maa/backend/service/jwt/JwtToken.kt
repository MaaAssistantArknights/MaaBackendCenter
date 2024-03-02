package plus.maa.backend.service.jwt

import cn.hutool.json.JSONObject
import cn.hutool.jwt.JWT
import cn.hutool.jwt.JWTUtil
import cn.hutool.jwt.RegisteredPayload
import java.time.Instant
import java.util.*

/**
 * 对 [JWT] 的包装增强，某些 payload 被标记为 MUST
 */
open class JwtToken {
    protected val jwt: JWT

    private val payload: JSONObject

    constructor(token: String, requiredType: String, key: ByteArray) {
        if (!JWTUtil.verify(token, key)) throw JwtInvalidException()
        this.jwt = JWTUtil.parseToken(token)
        jwt.setKey(key)
        this.payload = jwt.payloads

        // jwtId is nullable
        if (requiredType != type
            || payload.getStr(RegisteredPayload.SUBJECT) == null
            || payload.getLong(RegisteredPayload.ISSUED_AT) == null
            || payload.getLong(RegisteredPayload.EXPIRES_AT) == null
            || payload.getLong(RegisteredPayload.NOT_BEFORE) == null
        ) throw JwtInvalidException()
    }

    constructor(
        sub: String?,
        jti: String?,
        iat: Instant,
        exp: Instant,
        nbf: Instant,
        typ: String?,
        key: ByteArray
    ) {
        jwt = JWT.create()
        jwt.setPayload(RegisteredPayload.SUBJECT, sub)
        jwt.setPayload(RegisteredPayload.JWT_ID, jti)
        jwt.setPayload(RegisteredPayload.ISSUED_AT, iat.toEpochMilli())
        jwt.setPayload(RegisteredPayload.EXPIRES_AT, exp.toEpochMilli())
        jwt.setPayload(RegisteredPayload.NOT_BEFORE, nbf.toEpochMilli())
        jwt.setPayload(CLAIM_TYPE, typ)
        jwt.setKey(key)
        payload = jwt.payloads
    }

    val subject: String get() = payload.getStr(RegisteredPayload.SUBJECT)

    val jwtId: String? get() = payload.getStr(RegisteredPayload.JWT_ID)

    val issuedAt: Instant get() = Instant.ofEpochMilli(payload.getLong(RegisteredPayload.ISSUED_AT))

    val expiresAt: Instant get() = Instant.ofEpochMilli(payload.getLong(RegisteredPayload.EXPIRES_AT))

    val notBefore: Instant get() = Instant.ofEpochMilli(payload.getLong(RegisteredPayload.NOT_BEFORE))

    var type: String?
        get() = payload.getStr(CLAIM_TYPE)
        set(type) {
            payload[CLAIM_TYPE] = type
        }

    /**
     * 签名后的 jwt 字符串
     */
    val value: String get() = jwt.sign()

    @Throws(JwtExpiredException::class)
    fun validateDate(moment: Instant) {
        if (!moment.isBefore(expiresAt)) throw JwtExpiredException("expired")
        if (moment.isBefore(notBefore)) throw JwtExpiredException("haven't take effect")
    }

    companion object {
        private const val CLAIM_TYPE = "typ"
    }
}
