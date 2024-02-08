package plus.maa.backend.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.service.jwt.JwtService;

import java.io.IOException;

/**
 * @author AnselYuki
 */
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    public JwtAuthenticationTokenFilter(AuthenticationHelper helper, MaaCopilotProperties properties, JwtService jwtService) {
        this.helper = helper;
        this.properties = properties;
        this.jwtService = jwtService;
    }

    private final AuthenticationHelper helper;
    private final MaaCopilotProperties properties;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws IOException, ServletException {
        try {
            var token = extractToken(request);
            var authToken = jwtService.verifyAndParseAuthToken(token);
            helper.setAuthentication(authToken);
        } catch (Exception ex) {
            logger.trace(ex.getMessage());
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
}
