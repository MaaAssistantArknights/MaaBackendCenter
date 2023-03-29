package plus.maa.backend.config.security;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.context.SecurityContextHolder;
import plus.maa.backend.config.external.Jwt;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.service.UserSessionService;
import plus.maa.backend.service.model.LoginUser;

import java.util.HashMap;

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
        var now = DateTime.now();
        var newTime = now.offsetNew(DateField.SECOND, (int) properties.getJwt().getExpire());
        var userId = "test_user_id";
        var token = RandomStringUtils.random(16, true, true);
        var payload = new HashMap<String, Object>(4) {
            {
                put(JWTPayload.ISSUED_AT, now.getTime());
                put(JWTPayload.EXPIRES_AT, newTime.getTime());
                put(JWTPayload.NOT_BEFORE, now.getTime());
                put("userId", userId);
                put("token", token);
            }
        };
        var jwt = JWTUtil.createToken(payload, properties.getJwt().getSecret().getBytes());

        var userSessionService = mock(UserSessionService.class);
        var mockUser = new LoginUser();
        mockUser.setToken(token);
        when(userSessionService.getUser(userId)).thenReturn(mockUser);

        var filter = new JwtAuthenticationTokenFilter( new AuthenticationHelper(), properties, userSessionService);
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