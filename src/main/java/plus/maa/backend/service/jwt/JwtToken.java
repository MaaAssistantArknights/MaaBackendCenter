package plus.maa.backend.service.jwt;

import cn.hutool.core.date.DateTime;
import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.RegisteredPayload;
import lombok.Getter;

import java.util.Date;

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
            Date iat,
            Date exp,
            Date nbf,
            String typ,
            byte[] key
    ) {
        jwt = JWT.create();
        jwt.setPayload(RegisteredPayload.SUBJECT, sub);
        jwt.setPayload(RegisteredPayload.JWT_ID, jti);
        jwt.setPayload(RegisteredPayload.ISSUED_AT, iat.getTime());
        jwt.setPayload(RegisteredPayload.EXPIRES_AT, exp.getTime());
        jwt.setPayload(RegisteredPayload.NOT_BEFORE, nbf.getTime());
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


    public Date getIssuedAt() {
        return DateTime.of(payload.getLong(RegisteredPayload.ISSUED_AT));
    }


    public Date getExpiresAt() {
        return DateTime.of(payload.getLong(RegisteredPayload.EXPIRES_AT));
    }


    public Date getNotBefore() {
        return DateTime.of(payload.getLong(RegisteredPayload.NOT_BEFORE));
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

    public void validateDate(Date moment) throws JwtExpiredException {
        if (!moment.before(getExpiresAt())) throw new JwtExpiredException("expired");
        if (moment.before(getNotBefore())) throw new JwtExpiredException("haven't take effect");
    }

}
