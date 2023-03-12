package plus.maa.backend.service.jwt;

import cn.hutool.json.JSONObject;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.RegisteredPayload;
import lombok.Getter;

import java.util.Date;

/**
 * 对 {@link JWT} 的包装增强
 */
public class JwtToken {
    private static final String CLAIM_TYPE = "token_type";
    @Getter
    private final JWT jwt;

    private final JSONObject payload;

    public JwtToken(JWT jwt) {
        this.jwt = jwt;
        this.payload = jwt.getPayloads();
    }


    public String getSubject() {
        return payload.getStr(RegisteredPayload.SUBJECT);
    }


    public String getJwtId() {
        return payload.getStr(RegisteredPayload.JWT_ID);
    }


    public Date getIssuedAt() {
        return payload.getDate(RegisteredPayload.ISSUED_AT);
    }


    public Date getExpiresAt() {
        return payload.getDate(RegisteredPayload.EXPIRES_AT);
    }


    public Date getNotBefore() {
        return payload.getDate(RegisteredPayload.NOT_BEFORE);
    }


    public String getType() {
        return payload.getStr(CLAIM_TYPE);
    }

    public void setType(String type) {
        payload.set(CLAIM_TYPE, type);
    }

    /**
     * 生成 jwt 字符串
     * @return 签名后的 jwt 字符串
     */
    public String getValue() {
        return jwt.sign();
    }
}
