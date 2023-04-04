package plus.maa.backend.service.jwt;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
public class JwtService {
    private final Jwt jwtProperties;
    private final byte[] key;

    public JwtService(MaaCopilotProperties properties) {
        jwtProperties = properties.getJwt();
        key = jwtProperties.getSecret().getBytes();
    }

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
        var expiresAt = now.offsetNew(DateField.SECOND, (int) jwtProperties.getExpire());
        return new JwtAuthToken(subject, jwtId, now, expiresAt, now, authorities, key);
    }

    /**
     * 验证并解析为 AuthToken. 该方法为 stateless 的验证。
     *
     * @param authToken jwt 字符串
     * @return JwtAuthToken
     * @throws JwtInvalidException jwt不符合要求
     * @throws JwtExpiredException jwt未生效或者已过期
     */
    @NotNull
    public JwtAuthToken verifyAndParseAuthToken(String authToken) throws JwtInvalidException, JwtExpiredException {
        var token = new JwtAuthToken(authToken, key);
        token.validateDate(DateTime.now());
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
        var expiresAt = now.offsetNew(DateField.SECOND, (int) jwtProperties.getRefreshExpire());
        return new JwtRefreshToken(subject, jwtId, now, expiresAt, now, key);
    }

    /**
     * 产生新的 RefreshToken. 新的 token 除了签发和生效时间、 id 不同外，其余属性均继承自原来的 token.
     * 一般情况下， RefreshToken 应结合数据库使用以避免陷入无法撤销的窘境
     *
     * @param old 原 token
     * @return 新的 RefreshToken
     */
    @NotNull
    public JwtRefreshToken newRefreshToken(JwtRefreshToken old, @Nullable String jwtId) {
        var now = DateTime.now();
        return new JwtRefreshToken(old.getSubject(), jwtId, now, old.getExpiresAt(), now, key);
    }

    /**
     * 验证并解析为 RefreshToken. 该方法为 stateless 的验证。
     *
     * @param refreshToken jwt字符串
     * @return RefreshToken
     * @throws JwtInvalidException jwt不符合要求
     * @throws JwtExpiredException jwt未生效或者已过期
     */
    @NotNull
    public JwtRefreshToken verifyAndParseRefreshToken(String refreshToken) throws JwtInvalidException, JwtExpiredException {
        var token = new JwtRefreshToken(refreshToken, key);
        token.validateDate(DateTime.now());
        return token;
    }

}
