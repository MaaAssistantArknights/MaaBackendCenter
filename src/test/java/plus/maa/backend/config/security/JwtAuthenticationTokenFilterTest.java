package plus.maa.backend.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import plus.maa.backend.config.external.Jwt;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.service.jwt.JwtService;

import java.util.ArrayList;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
class JwtAuthenticationTokenFilterTest {

    @Test
    void testValidToken() {
        var properties = new MaaCopilotProperties();
        var jwtSettings = new Jwt();
        jwtSettings.setSecret("whatever you want");
        jwtSettings.setExpire(86400);
        properties.setJwt(jwtSettings);

        var jwtService = new JwtService(properties);
        var userId = "some user id";
        var authToken = jwtService.issueAuthToken(userId, null, new ArrayList<>());
        var jwt = authToken.getValue();

        var filter = new JwtAuthenticationTokenFilter(new AuthenticationHelper(), properties, jwtService);
        var request = mock(HttpServletRequest.class);
        when(request.getHeader(properties.getJwt().getHeader())).thenReturn("Bearer " + jwt);
        var filterChain = mock(FilterChain.class);
        try {
            filter.doFilter(request, mock(HttpServletResponse.class), filterChain);
        } catch (Exception ignored) {
        }
        assert SecurityContextHolder.getContext().getAuthentication() != null;
    }
}