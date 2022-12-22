package plus.maa.backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import plus.maa.backend.domain.MaaResult;
import plus.maa.backend.utils.WebUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author AnselYuki
 */
@Component
public class AuthenticationEntryPointImpl implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        MaaResult result = new MaaResult(HttpStatus.UNAUTHORIZED.value(), "认证失败请重新登录");
        String json = new ObjectMapper().writeValueAsString(result);
        WebUtils.renderString(response, json, HttpStatus.UNAUTHORIZED.value());
    }
}
