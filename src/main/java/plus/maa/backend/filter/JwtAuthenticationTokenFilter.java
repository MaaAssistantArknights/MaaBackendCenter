package plus.maa.backend.filter;

import java.io.IOException;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import com.fasterxml.jackson.databind.ObjectMapper;

import cn.hutool.core.date.DateTime;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.RegisteredPayload;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.service.model.LoginUser;

/**
 * @author AnselYuki
 */
@Setter
@Component
@RequiredArgsConstructor
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private final RedisCache redisCache;
    private final ObjectMapper objectMapper;
    @Value("${maa-copilot.jwt.header}")
    private String header;
    @Value("${maa-copilot.jwt.secret}")
    private String secret;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain) throws IOException, ServletException {
        String token = request.getHeader(header);
        if (!StringUtils.hasText(token)) {
            //未携带token，直接放行，交由后续过滤链处理
            filterChain.doFilter(request, response);
            return;
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        //解析token，用密钥验证token是否有效
        String redisKey;
        String jwtToken;
        try {
            if (!JWTUtil.verify(token, secret.getBytes())) {
                handleError(response, "验证失败");
                return;
            }
        } catch (Exception e) {
            handleError(response, "验证失败");
            return;
        }
        JWT jwt = JWTUtil.parseToken(token);
        jwtToken = jwt.getPayload("token").toString();
        DateTime now = DateTime.now();
        DateTime notBefore = DateTime.of((Long) jwt.getPayload(RegisteredPayload.NOT_BEFORE));
        DateTime expiresAt = DateTime.of((Long) jwt.getPayload(RegisteredPayload.EXPIRES_AT));
        if (!now.isBefore(expiresAt)) {
            handleError(response, "Token已过期");
            return;
        }
        if (!now.isAfter(notBefore)) {
            handleError(response, "Token还未生效");
            return;
        }
        redisKey = "LOGIN:" + jwt.getPayload("userId");

        //从redis中获取用户信息
        LoginUser loginUser = redisCache.getCache(redisKey, LoginUser.class);
        if (Objects.isNull(loginUser)) {
            handleError(response, "验证失败");
            return;
        }
        if (!Objects.equals(loginUser.getToken(), jwtToken)) {
            handleError(response, "验证失败");
            return;
        }

        //存入SecurityContext
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, null);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        //放行请求
        filterChain.doFilter(request, response);
    }

    private void handleError(HttpServletResponse response, String message) throws IOException {
        MaaResult<Void> result = new MaaResult<>(HttpServletResponse.SC_BAD_REQUEST, message, null);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setStatus(result.statusCode());
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
