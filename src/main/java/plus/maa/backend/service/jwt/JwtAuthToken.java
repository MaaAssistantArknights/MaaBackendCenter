package plus.maa.backend.service.jwt;

import cn.hutool.jwt.JWT;
import lombok.Getter;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.util.StringUtils;

import java.util.Collection;

/**
 * 基于 {@link JWT} 的 AuthToken. 本类实现了 {@link Authentication}， 可直接用于 Spring Security
 * 的认证流程
 */
@Getter
public final class JwtAuthToken extends JwtToken implements Authentication {
    /**
     * AuthToken 类型值
     */
    public static final String TYPE = "auth";
    private static final String CLAIM_AUTHORITIES = "Authorities";
    private boolean authenticated = false;

    private JwtAuthToken(JWT jwt) {
        super(jwt);
    }

    /**
     * 在 jwt 基础上构建
     *
     * @param jwt 待修改的 jwt
     * @return token
     */
    public static JwtAuthToken buildOn(JWT jwt) {
        var token = new JwtAuthToken(jwt);
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
    public static JwtAuthToken baseOn(JWT jwt) throws BadCredentialsException {
        var token = new JwtAuthToken(jwt);
        if (!TYPE.equals(token.getType())) throw new BadCredentialsException("invalid token");
        return token;
    }


    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        var authorityStrings = getJwt().getPayloads().getStr(CLAIM_AUTHORITIES);
        return StringUtils.commaDelimitedListToSet(authorityStrings).stream()
                .map(SimpleGrantedAuthority::new)
                .toList();
    }

    public void setAuthorities(Collection<? extends GrantedAuthority> authorities) {
        var authorityStrings = authorities.stream().map(GrantedAuthority::getAuthority).toList();
        var encodedAuthorities = StringUtils.collectionToCommaDelimitedString(authorityStrings);
        getJwt().setPayload(CLAIM_AUTHORITIES, encodedAuthorities);
    }

    /**
     * @return credentials，采用 jwt 的 id
     * @inheritDoc
     */
    @Override
    public Object getCredentials() {
        return getJwtId();
    }

    @Override
    public Object getDetails() {
        return null;
    }

    /**
     * @return principal，采用 jwt 的 subject
     * @inheritDoc
     */
    @Override
    public Object getPrincipal() {
        return getSubject();
    }

    @Override
    public boolean isAuthenticated() {
        return this.authenticated;
    }

    @Override
    public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {
        this.authenticated = isAuthenticated;
    }

    @Override
    public String getName() {
        return getSubject();
    }

}
