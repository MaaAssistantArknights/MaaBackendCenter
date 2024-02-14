package plus.maa.backend.service.jwt

import cn.hutool.json.JSONObject
import cn.hutool.jwt.JWT
import cn.hutool.jwt.JWTUtil
import cn.hutool.jwt.RegisteredPayload
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.*

/**
 * 对 [JWT] 的包装增强，某些 payload 被标记为 MUST
 */
open class JwtToken {
    protected val jwt: JWT

    private val payload: JSONObject

    constructor(token: String?, requiredType: String, key: ByteArray?) {
        if (!JWTUtil.verify(token, key)) throw JwtInvalidException()
        this.jwt = JWTUtil.parseToken(token)
        jwt.setKey(key)
        this.payload = jwt.payloads

        // jwtId is nullable
        if (requiredType != type) throw JwtInvalidException()
    }

    constructor(
        sub: String?,
        jti: String?,
        iat: LocalDateTime,
        exp: LocalDateTime,
        nbf: LocalDateTime,
        typ: String?,
        key: ByteArray?
    ) {
        jwt = JWT.create()
        jwt.setPayload(RegisteredPayload.SUBJECT, sub)
        jwt.setPayload(RegisteredPayload.JWT_ID, jti)
        jwt.setPayload(RegisteredPayload.ISSUED_AT, iat.toInstant(OffsetDateTime.now().offset).toEpochMilli())
        jwt.setPayload(RegisteredPayload.EXPIRES_AT, exp.toInstant(OffsetDateTime.now().offset).toEpochMilli())
        jwt.setPayload(RegisteredPayload.NOT_BEFORE, nbf.toInstant(OffsetDateTime.now().offset).toEpochMilli())
        jwt.setPayload(CLAIM_TYPE, typ)
        jwt.setKey(key)
        payload = jwt.payloads
    }


    val subject: String
        get() = payload.getStr(RegisteredPayload.SUBJECT)


    val jwtId: String
        get() = payload.getStr(RegisteredPayload.JWT_ID)


    val issuedAt: LocalDateTime
        get() = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(payload.getLong(RegisteredPayload.ISSUED_AT)),
            TimeZone.getDefault().toZoneId()
        )


    val expiresAt: LocalDateTime
        get() = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(payload.getLong(RegisteredPayload.EXPIRES_AT)),
            TimeZone.getDefault().toZoneId()
        )


    val notBefore: LocalDateTime
        get() = LocalDateTime.ofInstant(
            Instant.ofEpochMilli(payload.getLong(RegisteredPayload.NOT_BEFORE)),
            TimeZone.getDefault().toZoneId()
        )


    var type: String?
        get() = payload.getStr(CLAIM_TYPE)
        set(type) {
            payload[CLAIM_TYPE] = type
        }

    val value: String
        /**
         * 生成 jwt 字符串
         *
         * @return 签名后的 jwt 字符串
         */
        get() = jwt.sign()

    @Throws(JwtExpiredException::class)
    fun validateDate(moment: LocalDateTime) {
        if (!moment.isBefore(expiresAt)) throw JwtExpiredException("expired")
        if (moment.isBefore(notBefore)) throw JwtExpiredException("haven't take effect")
    }

    companion object {
        private const val CLAIM_TYPE = "typ"
    }
}
