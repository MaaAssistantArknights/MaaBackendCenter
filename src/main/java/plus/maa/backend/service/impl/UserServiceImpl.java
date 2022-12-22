package plus.maa.backend.service.impl;

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
import org.springframework.stereotype.Service;
import plus.maa.backend.domain.LoginUser;
import plus.maa.backend.domain.MaaResult;
import plus.maa.backend.model.MaaUser;
import plus.maa.backend.repository.UserRepository;
import plus.maa.backend.service.UserService;
import plus.maa.backend.utils.RedisCache;
import plus.maa.backend.vo.LoginVo;
import plus.maa.backend.vo.MaaUserInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * @author AnselYuki
 */
@Setter
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final AuthenticationManager authenticationManager;
    private final RedisCache redisCache;
    private final UserRepository userRepository;
    @Value("${maa-copilot.jwt.secret}")
    private String secret;
    @Value("${maa-copilot.jwt.expire}")
    private int expire;

    @Override
    public MaaResult<Map<String, String>> login(LoginVo user) {
        //使用 AuthenticationManager 中的 authenticate 进行用户认证
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword());
        Authentication authenticate = authenticationManager.authenticate(authenticationToken);
        //若认证失败，给出相应提示
        if (Objects.isNull(authenticate)) {
            throw new RuntimeException("登陆失败");
        }
        //若认证成功，使用UserID生成一个JwtToken,Token存入ResponseResult返回
        LoginUser principal = (LoginUser) authenticate.getPrincipal();
        String userId = String.valueOf(principal.getUser().getEmail());
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

    @Override
    public MaaResult<MaaUserInfo> findUserInfoById(String id) {
        Optional<MaaUser> user = userRepository.findById(id);
        if (user.isPresent()) {
            MaaUserInfo userInfo = new MaaUserInfo();
            BeanUtils.copyProperties(user.get(), userInfo);
            return MaaResult.success(userInfo);
        }
        return MaaResult.fail(10002, "找不到用户");
    }

    @Override
    public MaaResult<Void> addUser(MaaUser user) {
        try {
            userRepository.insert(user);
        } catch (DuplicateKeyException e) {
            return MaaResult.fail(10001, "添加用户失败");
        }
        return MaaResult.success(null);
    }
}
