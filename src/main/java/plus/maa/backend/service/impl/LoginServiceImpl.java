package plus.maa.backend.service.impl;

import plus.maa.backend.domain.LoginUser;
import plus.maa.backend.domain.ResponseResult;
import plus.maa.backend.service.LoginService;
import plus.maa.backend.utils.RedisCache;
import plus.maa.backend.vo.LoginVo;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author AnselYuki
 */
@Setter
@Service
public class LoginServiceImpl implements LoginService {
    private final AuthenticationManager authenticationManager;
    private final RedisCache redisCache;
    @Value("${anselyuki.jwt.secret}")
    private String secret;
    @Value("${anselyuki.jwt.expire}")
    private int expire;

    @Autowired
    public LoginServiceImpl(AuthenticationManager authenticationManager, RedisCache redisCache) {
        this.authenticationManager = authenticationManager;
        this.redisCache = redisCache;
    }

    @Override
    public ResponseResult<Map<String, String>> login(LoginVo user) {
        //使用 AuthenticationManager 中的 authenticate 进行用户认证
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        //若认证失败，给出相应提示
        if (Objects.isNull(authenticate)) {
            throw new RuntimeException("登陆失败");
        }
        //若认证成功，使用UserID生成一个JwtToken,Token存入ResponseResult返回
        LoginUser principal = (LoginUser) authenticate.getPrincipal();
        String userId = String.valueOf(principal.getUser().getId());
        DateTime now = DateTime.now();
        DateTime newTime = now.offsetNew(DateField.SECOND, expire);
        //签发JwtToken，从上到下为设置签发时间，过期时间与生效时间
        Map<String, Object> payload = new HashMap<>(4) {
            {
                put(JWTPayload.ISSUED_AT, now.getTime());
                put(JWTPayload.EXPIRES_AT, newTime.getTime());
                put(JWTPayload.NOT_BEFORE, now.getTime());
                put("userId", userId);
            }
        };
        String token = JWTUtil.createToken(payload, secret.getBytes());
        //把完整的用户信息存入Redis，UserID作为Key
        redisCache.setCacheLoginUser("LOGIN:" + userId, principal, expire, TimeUnit.SECONDS);
        return new ResponseResult<>(200, "登录成功", Map.of("token", token));
    }

    @Override
    public ResponseResult<?> loginOut(String token) {
        //获取token中的用户信息
        JWT jwt = JWTUtil.parseToken(token);
        String id = (String) jwt.getPayload("userId");
        //删除Redis中的值
        redisCache.deleteLoginUser("LOGIN:" + id);
        return new ResponseResult<>(200, "注销成功");
    }
}
