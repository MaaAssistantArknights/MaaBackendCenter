package plus.maa.backend.filter;

import cn.hutool.core.date.DateTime;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import cn.hutool.jwt.RegisteredPayload;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import plus.maa.backend.domain.LoginUser;
import plus.maa.backend.utils.RedisCache;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;

/**
 * @author AnselYuki
 */
@Slf4j
@Setter
@Component
public class JwtAuthenticationTokenFilter extends OncePerRequestFilter {
    private final RedisCache redisCache;
    @Value("${maa-copilot.jwt.header}")
    private String header;
    @Value("${maa-copilot.jwt.secret}")
    private String secret;

    @Autowired
    public JwtAuthenticationTokenFilter(RedisCache redisCache) {
        this.redisCache = redisCache;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String token = request.getHeader(header);
        if (!StringUtils.hasText(token)) {
            //未携带token，直接放行，交由后续过滤链处理
            filterChain.doFilter(request, response);
            return;
        }
        //解析token，用密钥验证token是否有效
        String redisKey;
        if (!JWTUtil.verify(token, secret.getBytes())) {
            JWT jwt = JWTUtil.parseToken(token);
            DateTime now = DateTime.now();
            DateTime notBefore = DateTime.of((Long) jwt.getPayload(RegisteredPayload.NOT_BEFORE));
            DateTime expiresAt = DateTime.of((Long) jwt.getPayload(RegisteredPayload.EXPIRES_AT));
            if (!now.isBefore(expiresAt)) {
                throw new RuntimeException("Token已过期");
            }
            if (!now.isAfter(notBefore)) {
                throw new RuntimeException("Token还未生效");
            }
            redisKey = "LOGIN:" + jwt.getPayload("userId");
        } else {
            throw new RuntimeException("验证失败");
        }
        //从redis中获取用户信息
        LoginUser loginUser = redisCache.getCacheLoginUser(redisKey);
        if (Objects.isNull(loginUser)) {
            throw new RuntimeException("验证失败");
        }
        //存入SecurityContext
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        //放行请求
        filterChain.doFilter(request, response);
    }
}
