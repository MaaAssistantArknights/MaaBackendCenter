package plus.maa.backend.config.security;

import cn.hutool.core.lang.Assert;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import plus.maa.backend.common.utils.WebUtils;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.user.MaaLoginRsp;
import plus.maa.backend.repository.UserRepository;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.UserService;

import java.io.IOException;

/**
 * 适配 Maa Account
 *
 * @author lixuhuilll
 * Date 2023/9/22
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OIDCAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final UserService userService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        try {
            authenticationSuccess(request, response, authentication);
        } catch (AuthenticationException e) {
            throw e;
        } catch (RuntimeException e) {
            // 将运行时异常转换为 AuthenticationException 的子类型，触发统一的异常响应
            throw new OIDCAuthenticationException(e.getMessage());
        } finally {
            // 删除在身份验证过程中可能已存储在会话中的临时身份验证相关数据
            clearAuthenticationAttributes(request);
        }
    }

    public void authenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauth2Token)) {
            throw new OIDCAuthenticationException("无法取得授权信息");
        }

        OAuth2User oAuth2User = oauth2Token.getPrincipal();

        String email = oAuth2User.getAttribute("email");
        Assert.notBlank(email, "无法取得邮箱");

        MaaUser maaUser = userRepository.findByEmail(email);
        if (maaUser == null) {
            // 如果不存在绑定好的邮箱，则注册新用户
            String userName = oAuth2User.getAttribute("preferred_username");
            Assert.notBlank(userName, "无法取得用户名");

            maaUser = new MaaUser()
                    .setUserName(userName)
                    .setEmail(email)
                    .setStatus(1);
            maaUser = userRepository.save(maaUser);
        } else if (maaUser.getStatus() == null || maaUser.getStatus() == 0) {
            // 存在对应邮箱的用户但未激活时，自动激活
            maaUser.setStatus(1);
            userRepository.save(maaUser);
        }

        // 响应登录数据
        MaaResult<MaaLoginRsp> result = MaaResult.success("登陆成功", userService.maaLoginRsp(maaUser));
        String json = objectMapper.writeValueAsString(result);
        WebUtils.renderString(response, json, 200);
    }

    static class OIDCAuthenticationException extends AuthenticationException {
        public OIDCAuthenticationException(String msg) {
            super(msg);
        }
    }
}
