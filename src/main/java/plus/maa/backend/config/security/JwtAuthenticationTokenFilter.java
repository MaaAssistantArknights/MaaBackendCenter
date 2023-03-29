package plus.maa.backend.config.security;

import cn.hutool.core.date.DateTime;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.RegisteredPayload;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.service.UserSessionService;
import plus.maa.backend.service.model.LoginUser;

import java.io.IOException;
import java.util.Objects;

/**
 * @author AnselYuki
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    public JwtAuthenticationTokenFilter(AuthenticationHelper helper, MaaCopilotProperties properties, UserSessionService userSessionService) {
        this.helper = helper;
        this.properties = properties;
        this.userSessionService = userSessionService;
    }

    private final AuthenticationHelper helper;
    private final MaaCopilotProperties properties;

    private final UserSessionService userSessionService;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws IOException, ServletException {
        try {
            var token = extractToken(request);
            var jwt = parseAndValidateJwt(token);
            var user = retrieveAndValidateUser(jwt);
            var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
            helper.setAuthentication(authentication);
        } catch (AuthenticationException ex) {
            logger.trace(ex.getMessage());
        } catch (Exception ignored) {
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    @NotNull
    private String extractToken(HttpServletRequest request) throws Exception {
        if (SecurityContextHolder.getContext().getAuthentication() != null) throw new Exception("no need to auth");
        var head = request.getHeader(properties.getJwt().getHeader());
        if (head == null || !head.startsWith("Bearer ")) throw new Exception("token not found");
        return head.substring(7);
    }

    @NotNull
    private JWT parseAndValidateJwt(String token) throws BadCredentialsException {
        if (!JWTUtil.verify(token, properties.getJwt().getSecret().getBytes()))
            throw new BadCredentialsException("invalid token");
        var jwt = JWTUtil.parseToken(token);
        var now = DateTime.now();
        var notBefore = DateTime.of((Long) jwt.getPayload(RegisteredPayload.NOT_BEFORE));
        if (now.isBefore(notBefore)) throw new CredentialsExpiredException("haven't come into effect");
        var expiresAt = DateTime.of((Long) jwt.getPayload(RegisteredPayload.EXPIRES_AT));
        if (now.isAfter(expiresAt)) throw new CredentialsExpiredException("token expired");
        return jwt;
    }

    @NotNull
    private LoginUser retrieveAndValidateUser(JWT jwt) throws AuthenticationException {
        var user = userSessionService.getUser(jwt.getPayload("userId").toString());
        if (user == null) throw new UsernameNotFoundException("user not found");
        var jwtToken = jwt.getPayload("token").toString();
        if (!Objects.equals(user.getToken(), jwtToken)) throw new BadCredentialsException("invalid token");
        return user;
    }
}
