package plus.maa.backend.service.jwt;

import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.RegisteredPayload;
import lombok.Getter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.TimeZone;

/**
 * 对 {@link JWT} 的包装增强，某些 payload 被标记为 MUST
 */
public class JwtToken {
    private static final String CLAIM_TYPE = "typ";
    @Getter
    private final JWT jwt;

    private final JSONObject payload;

    public JwtToken(String token, String requiredType, byte[] key) throws JwtInvalidException {
        if (!JWTUtil.verify(token, key)) throw new JwtInvalidException();
        this.jwt = JWTUtil.parseToken(token);
        this.jwt.setKey(key);
        this.payload = jwt.getPayloads();

        // jwtId is nullable
        if (null == getSubject()
                || null == getIssuedAt()
                || null == getExpiresAt()
                || null == getNotBefore()
                || !requiredType.equals(getType())
        ) throw new JwtInvalidException();
    }

    public JwtToken(
            String sub,
            String jti,
            LocalDateTime iat,
            LocalDateTime  exp,
            LocalDateTime   nbf,
            String typ,
            byte[] key
    ) {
        jwt = JWT.create();
        jwt.setPayload(RegisteredPayload.SUBJECT, sub);
        jwt.setPayload(RegisteredPayload.JWT_ID, jti);
        jwt.setPayload(RegisteredPayload.ISSUED_AT, iat.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli());
        jwt.setPayload(RegisteredPayload.EXPIRES_AT, exp.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli());
        jwt.setPayload(RegisteredPayload.NOT_BEFORE, nbf.toInstant(OffsetDateTime.now().getOffset()).toEpochMilli());
        jwt.setPayload(CLAIM_TYPE, typ);
        jwt.setKey(key);
        payload = jwt.getPayloads();
    }


    public String getSubject() {
        return payload.getStr(RegisteredPayload.SUBJECT);
    }


    public String getJwtId() {
        return payload.getStr(RegisteredPayload.JWT_ID);
    }


    public LocalDateTime getIssuedAt() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(payload.getLong(RegisteredPayload.ISSUED_AT)),
                TimeZone.getDefault().toZoneId());
    }


    public LocalDateTime getExpiresAt() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(payload.getLong(RegisteredPayload.EXPIRES_AT)),
                TimeZone.getDefault().toZoneId());
    }


    public LocalDateTime getNotBefore() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(payload.getLong(RegisteredPayload.NOT_BEFORE)),
                TimeZone.getDefault().toZoneId());
    }


    public String getType() {
        return payload.getStr(CLAIM_TYPE);
    }

    public void setType(String type) {
        payload.set(CLAIM_TYPE, type);
    }

    /**
     * 生成 jwt 字符串
     *
     * @return 签名后的 jwt 字符串
     */
    public String getValue() {
        return jwt.sign();
    }

    public void validateDate(LocalDateTime moment) throws JwtExpiredException {
        if (!moment.isBefore(getExpiresAt())) throw new JwtExpiredException("expired");
        if (moment.isBefore(getNotBefore())) throw new JwtExpiredException("haven't take effect");
    }

}
