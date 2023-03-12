package plus.maa.backend.service.jwt;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.exceptions.ValidateException;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.JWTValidator;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import plus.maa.backend.config.external.Jwt;
import plus.maa.backend.config.external.MaaCopilotProperties;

import java.util.Collection;

/**
 * 基于 Jwt 的 token 服务。 可直接用于 stateless 情境下的签发和认证， 或结合数据库进行状态管理。
 * 建议 AuthToken 使用无状态方案, RefreshToken 使用有状态方案
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final MaaCopilotProperties properties;

    /**
     * 签发 AuthToken. 过期时间由配置的 {@link Jwt#getExpire()} 计算而来
     *
     * @param subject     签发对象，一般设置为对象的标识符
     * @param jwtId       jwt 的 id， 一般用于 stateful 场景下
     * @param authorities 授予的权限
     * @return JwtAuthToken
     */
    public JwtAuthToken issueAuthToken(String subject, @Nullable String jwtId, Collection<? extends GrantedAuthority> authorities) {
        var now = DateTime.now();
        var expiresAt = now.offsetNew(DateField.SECOND, (int) properties.getJwt().getExpire());
        var jwt = JWT.create()
                .setIssuedAt(now)
                .setExpiresAt(expiresAt)
                .setNotBefore(now)
                .setSubject(subject)
                .setJWTId(jwtId)
                .setKey(properties.getJwt().getSecret().getBytes());
        var token = JwtAuthToken.buildOn(jwt);
        token.setAuthorities(authorities);
        return token;
    }

    /**
     * 验证并解析为 AuthToken. 该方法为 stateless 的验证。
     *
     * @param authToken jwt 字符串
     * @return JwtAuthToken
     * @throws AuthenticationException 验证失败
     */
    @NotNull
    public JwtAuthToken verifyAndParseAuthToken(String authToken) throws AuthenticationException {
        var jwt = verifyAndParseJwt(authToken);
        var token = JwtAuthToken.baseOn(jwt);
        token.setAuthenticated(true);
        return token;
    }

    /**
     * 签发 RefreshToken. 过期时间由配置的 {@link Jwt#getRefreshExpire()} 计算而来
     *
     * @param subject 签发对象，一般设置为对象的标识符
     * @param jwtId   jwt 的 id， 一般用于 stateful 场景下
     * @return JwtAuthToken
     */
    @NotNull
    public JwtRefreshToken issueRefreshToken(String subject, @Nullable String jwtId) {
        var now = DateTime.now();
        var expiresAt = now.offsetNew(DateField.SECOND, (int) properties.getJwt().getRefreshExpire());
        var jwt = JWT.create()
                .setIssuedAt(now)
                .setExpiresAt(expiresAt)
                .setNotBefore(now)
                .setSubject(subject)
                .setJWTId(jwtId)
                .setKey(properties.getJwt().getSecret().getBytes());
        return JwtRefreshToken.buildOn(jwt);
    }

    /**
     * 产生新的 RefreshToken. 新的 token 除了签发和生效时间不同外，其余属性均继承自原来的 token.
     * 一般情况下， RefreshToken 应结合数据库使用以避免陷入无法撤销的窘境
     *
     * @param old 原 token
     * @return 新的 RefreshToken
     */
    @NotNull
    public JwtRefreshToken newRefreshToken(JwtRefreshToken old) {
        var now = DateTime.now();
        var jwt = JWT.create()
                .setIssuedAt(now)
                .setExpiresAt(old.getExpiresAt())
                .setNotBefore(now)
                .setSubject(old.getSubject())
                .setJWTId(old.getJwtId())
                .setKey(properties.getJwt().getSecret().getBytes());
        return JwtRefreshToken.buildOn(jwt);
    }

    /**
     * 验证并解析为 RefreshToken. 该方法为 stateless 的验证。
     *
     * @param refreshToken jwt字符串
     * @return RefreshToken
     * @throws AuthenticationException 验证失败
     */
    @NotNull
    public JwtRefreshToken verifyAndParseRefreshToken(String refreshToken) throws AuthenticationException {
        var jwt = verifyAndParseJwt(refreshToken);
        return JwtRefreshToken.baseOn(jwt);
    }

    /**
     * 验证并解析为 {@link JWT}. 该方法验证签名的正确性和时间的合法性。
     *
     * @param token jwt 字符串
     * @return 生成的 JWT
     * @throws AuthenticationException 验证失败
     */
    @NotNull
    private JWT verifyAndParseJwt(String token) throws AuthenticationException {
        if (!JWTUtil.verify(token, properties.getJwt().getSecret().getBytes()))
            throw new BadCredentialsException("invalid token");

        var jwt = JWTUtil.parseToken(token);
        try {
            JWTValidator.of(jwt).validateDate(DateTime.now());
        } catch (ValidateException e) {
            throw new CredentialsExpiredException("expired", e);
        }
        return jwt;
    }
}
