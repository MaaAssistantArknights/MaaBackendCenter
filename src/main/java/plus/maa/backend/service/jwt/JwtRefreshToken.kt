package plus.maa.backend.service.jwt;

import java.time.LocalDateTime;

public class JwtRefreshToken extends JwtToken {
    /**
     * RefreshToken 类型值
     */
    public static final String TYPE = "refresh";

    /**
     * 从 jwt 构建 token
     *
     * @param token jwt
     * @param key   签名密钥
     * @throws JwtInvalidException jwt 未通过签名验证或不符合要求
     */
    public JwtRefreshToken(String token, byte[] key) throws JwtInvalidException {
        super(token, TYPE, key);
    }

    public JwtRefreshToken(
            String sub,
            String jti,
            LocalDateTime iat,
            LocalDateTime  exp,
            LocalDateTime   nbf,
            byte[] key
    ) {
        super(sub, jti, iat, exp, nbf, TYPE, key);
    }

}
