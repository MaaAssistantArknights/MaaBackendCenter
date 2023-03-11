package plus.maa.backend.service.jwt;

import cn.hutool.jwt.JWT;
import org.springframework.security.authentication.BadCredentialsException;

public class JwtRefreshToken extends JwtToken {
    /**
     * RefreshToken 类型值
     */
    public static final String TYPE = "refresh";

    private JwtRefreshToken(JWT jwt) {
        super(jwt);
    }

    /**
     * 在 jwt 基础上构建
     *
     * @param jwt 待修改的 jwt
     * @return token
     */
    public static JwtRefreshToken buildOn(JWT jwt) {
        var token = new JwtRefreshToken(jwt);
        token.setType(TYPE);
        return token;
    }

    /**
     * 构建jwt的直接包装
     *
     * @param jwt 解析而来的 jwt
     * @return token
     * @throws BadCredentialsException jwt 验证失败
     */
    public static JwtRefreshToken baseOn(JWT jwt) throws BadCredentialsException {
        var token = new JwtRefreshToken(jwt);
        if (!TYPE.equals(token.getType())) throw new BadCredentialsException("invalid token");
        return token;
    }

}
