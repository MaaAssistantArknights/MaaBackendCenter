package plus.maa.backend.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.log.LogMessage;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.stereotype.Component;
import plus.maa.backend.common.utils.WebUtils;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.user.OIDCInfo;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OIDCRedirectStrategy extends DefaultRedirectStrategy {

    private final ObjectMapper objectMapper;

    @Override
    public void sendRedirect(HttpServletRequest request, HttpServletResponse response, String url) throws IOException {
        String redirectUrl = calculateRedirectUrl(request.getContextPath(), url);
        redirectUrl = response.encodeRedirectURL(redirectUrl);
        if (this.logger.isDebugEnabled()) {
            this.logger.debug(LogMessage.format("Redirecting to %s", redirectUrl));
        }
        // 不再重定向，而是响应流水号和目标地址
        String serial = (String) request.getAttribute(RedisOAuth2AuthorizationRequestRepository.getREQUEST_KEY());
        OIDCInfo oidcInfo = new OIDCInfo(serial, redirectUrl);
        MaaResult<OIDCInfo> result = MaaResult.success(oidcInfo);
        String json = objectMapper.writeValueAsString(result);
        WebUtils.renderString(response, json, 200);
    }
}
