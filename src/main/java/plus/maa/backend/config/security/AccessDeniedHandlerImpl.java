package plus.maa.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import plus.maa.backend.common.utils.WebUtils;
import plus.maa.backend.controller.response.MaaResult;

import java.io.IOException;

/**
 * @author AnselYuki
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        var result = MaaResult.fail(HttpStatus.FORBIDDEN.value(), "权限不足");
        String json = new ObjectMapper().writeValueAsString(result);
        WebUtils.renderString(response, json, HttpStatus.FORBIDDEN.value());
    }
}
