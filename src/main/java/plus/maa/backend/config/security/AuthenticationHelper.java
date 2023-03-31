package plus.maa.backend.config.security;

import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;
import plus.maa.backend.common.utils.IpUtil;
import plus.maa.backend.service.jwt.JwtAuthToken;
import plus.maa.backend.service.model.LoginUser;

import java.util.Objects;

/**
 * Auth 助手，统一 auth 的设置和获取
 */
@Component
public class AuthenticationHelper {
    /**
     * 设置当前 auth， 是 SecurityContextHolder.getContext().setAuthentication(authentication) 的集中调用
     *
     * @param authentication 当前的 auth
     */
    public void setAuthentication(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /**
     * 要求用户 id ，否则抛出异常
     *
     * @return 已经验证的用户 id
     * @throws ResponseStatusException 用户未通过验证
     */
    public @NotNull String requireUserId() throws ResponseStatusException {
        var id = getUserId();
        if (id == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return id;
    }

    /**
     * 获取用户 id
     *
     * @return 用户 id，如未验证则返回 null
     */
    public @Nullable String getUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth instanceof UsernamePasswordAuthenticationToken) {
            var principal = auth.getPrincipal();
            if (principal instanceof LoginUser) return ((LoginUser) principal).getUserId();
        } else if (auth instanceof JwtAuthToken) {
            return ((JwtAuthToken) auth).getSubject();
        }
        return null;
    }

    /**
     * 获取已验证用户 id 或者未验证用户 ip 地址。在 HTTP request 之外调用该方法获取 ip 会抛出 NPE
     *
     * @return 用户 id 或者 ip 地址
     */
    public @NotNull String getUserIdOrIpAddress() {
        var attributes = ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes()));
        var request = attributes.getRequest();
        var id = getUserId();
        if (id == null) id = IpUtil.getIpAddr(request);
        return id;
    }
}
