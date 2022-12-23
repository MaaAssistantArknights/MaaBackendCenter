package plus.maa.backend.service;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import plus.maa.backend.controller.request.LoginRequest;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaUserInfo;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.UserRepository;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.model.LoginUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author AnselYuki
 */
@Setter
@Service
@RequiredArgsConstructor
public class UserService {
    private final AuthenticationManager authenticationManager;
    private final RedisCache redisCache;
    private final UserRepository userRepository;
    @Value("${maa-copilot.jwt.secret}")
    private String secret;
    @Value("${maa-copilot.jwt.expire}")
    private int expire;

    public MaaResult<Map<String, String>> login(LoginRequest loginVo) {
        //使用 AuthenticationManager 中的 authenticate 进行用户认证
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(loginVo.getEmail(), loginVo.getPassword());
        Authentication authenticate;
        try {
            authenticate = authenticationManager.authenticate(authenticationToken);
        } catch (Exception e) {
            throw new MaaResultException(403, "用户名或密码不正确");
        }
        //若认证失败，给出相应提示
        if (Objects.isNull(authenticate)) {
            throw new MaaResultException("登陆失败");
        }
        //若认证成功，使用UserID生成一个JwtToken,Token存入ResponseResult返回
        LoginUser principal = (LoginUser) authenticate.getPrincipal();
        String userId = String.valueOf(principal.getMaaUser().getId());
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
        return MaaResult.success("登录成功", Map.of("token", token));
    }

    public MaaResult<MaaUserInfo> findUserInfoById(String id) {
        MaaUser user = userRepository.findById(id).orElse(null);
        if (!Objects.isNull(user)) {
            MaaUserInfo userInfo = new MaaUserInfo();
            BeanUtils.copyProperties(user, userInfo);
            return MaaResult.success(userInfo);
        }
        throw new MaaResultException(10002, "找不到用户");
    }

    public MaaResult<Void> addUser(MaaUser user) {
        String rawPassword = user.getPassword();
        String encode = new BCryptPasswordEncoder().encode(rawPassword);
        user.setPassword(encode);
        try {
            userRepository.save(user);
        } catch (DuplicateKeyException e) {
            throw new MaaResultException(10001, "添加用户失败");
        }
        return MaaResult.success(null);
    }
}
