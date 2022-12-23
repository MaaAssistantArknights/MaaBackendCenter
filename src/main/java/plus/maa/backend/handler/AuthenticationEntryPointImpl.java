package plus.maa.backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.utils.WebUtils;

import java.io.IOException;

/**
 * @author AnselYuki
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        MaaResult<Void> result = MaaResult.fail(HttpStatus.UNAUTHORIZED.value(), "认证失败请重新登录");
        String json = new ObjectMapper().writeValueAsString(result);
        WebUtils.renderString(response, json, HttpStatus.UNAUTHORIZED.value());
    }
}
