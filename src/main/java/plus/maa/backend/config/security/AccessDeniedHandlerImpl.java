package plus.maa.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import plus.maa.backend.common.utils.WebUtils;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.model.LoginUser;

import java.io.IOException;

/**
 * @author AnselYuki
 */
@Component
public class AccessDeniedHandlerImpl implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException {
        //获取当前用户的权限
        LoginUser user = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        MaaResult<Void> result;
        if (user.getMaaUser().getStatus() == 0) {
            result = MaaResult.fail(HttpStatus.FORBIDDEN.value(), "当前账户尚未激活》请先通过邮件信息激活账户");
        } else {
            result = MaaResult.fail(HttpStatus.FORBIDDEN.value(), "权限不足");
        }
        String json = new ObjectMapper().writeValueAsString(result);
        WebUtils.renderString(response, json, HttpStatus.FORBIDDEN.value());
    }
}
