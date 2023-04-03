package plus.maa.backend.service;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Assert;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.MaaStatusCode;
import plus.maa.backend.common.utils.converter.MaaUserConverter;
import plus.maa.backend.controller.request.*;
import plus.maa.backend.controller.response.MaaLoginRsp;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.controller.response.MaaUserInfo;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.UserRepository;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.session.UserSessionService;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author AnselYuki
 */
@Setter
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final RedisCache redisCache;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final UserSessionService userSessionService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailServiceImpl userDetailService;

    @Value("${maa-copilot.jwt.secret}")
    private String secret;
    @Value("${maa-copilot.jwt.expire}")
    private int expire;
    @Value("${maa-copilot.vcode.expire:600}")
    private int registrationCodeExpireInSecond;

    /**
     * 登录方法
     *
     * @param loginDTO 登录参数
     * @return 携带了token的封装类
     */
    public MaaLoginRsp login(LoginDTO loginDTO) {
        var user = userRepository.findByEmail(loginDTO.getEmail());
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword()))
            throw new MaaResultException(401, "登陆失败");

        String userId = String.valueOf(user.getUserId());
        String token = RandomStringUtils.random(16, true, true);
        DateTime now = DateTime.now();
        DateTime newTime = now.offsetNew(DateField.SECOND, expire);
        // 签发JwtToken，从上到下为设置签发时间，过期时间与生效时间
        Map<String, Object> payload = new HashMap<>(4) {
            {
                put(JWTPayload.ISSUED_AT, now.getTime());
                put(JWTPayload.EXPIRES_AT, newTime.getTime());
                put(JWTPayload.NOT_BEFORE, now.getTime());
                put("userId", userId);
                put("token", token);
            }
        };

        userSessionService.setSession(user.getUserId(), (session) -> {
            session.setMaaUser(user);
            session.setPermissions(userDetailService.collectPermissionsFor(user));
            session.setToken(token);
        });

        String jwt = JWTUtil.createToken(payload, secret.getBytes());

        MaaLoginRsp rsp = new MaaLoginRsp();
        rsp.setToken(jwt);
        rsp.setValidAfter(now);
        rsp.setValidBefore(newTime);
        rsp.setRefreshToken("");
        rsp.setRefreshTokenValidBefore("");
        rsp.setUserInfo(MaaUserConverter.INSTANCE.convert(user));

        return rsp;
    }

    /**
     * 修改密码
     *
     * @param userId      当前用户
     * @param rawPassword 新密码
     */
    public void modifyPassword(String userId, String rawPassword) {
        var userResult = userRepository.findById(userId);
        if (userResult.isEmpty()) return;
        var maaUser = userResult.get();
        // 修改密码的逻辑，应当使用与 authentication provider 一致的编码器
        maaUser.setPassword(passwordEncoder.encode(rawPassword));
        userRepository.save(maaUser);

        userSessionService.updateSessionIfPresent(userId, (session) -> {
            session.setMaaUser(maaUser);
            session.setToken(RandomStringUtils.random(16, true, true));
        });
        // TODO 更新jwt-token并重新签发jwt, 通知客户端更新jwt

    }

    /**
     * 用户注册
     *
     * @param registerDTO 传入用户参数
     * @return 返回注册成功的用户摘要（脱敏）
     */
    public MaaUserInfo register(RegisterDTO registerDTO) {
        String encode = passwordEncoder.encode(registerDTO.getPassword());
        MaaUser user = new MaaUser();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(encode);
        user.setStatus(1);
        MaaUserInfo userInfo;
        if (!emailService.verifyVCode2(user.getEmail(), registerDTO.getRegistrationToken(), false)) {
            throw new MaaResultException(MaaStatusCode.MAA_REGISTRATION_CODE_NOT_FOUND);
        }
        try {
            MaaUser save = userRepository.save(user);
            userInfo = new MaaUserInfo(save);
        } catch (DuplicateKeyException e) {
            throw new MaaResultException(MaaStatusCode.MAA_USER_EXISTS);
        }
        return userInfo;
    }

    /**
     * 通过传入的JwtToken来获取当前用户的信息
     *
     * @param userId      当前用户
     * @param activateDTO 邮箱激活码
     */
    public void activateUser(@NotNull String userId, ActivateDTO activateDTO) {
        userRepository.findById(userId).ifPresent((maaUser) -> {
            if (1 == maaUser.getStatus()) return;
            var email = maaUser.getEmail();
            emailService.verifyVCode(email, activateDTO.getToken());
            maaUser.setStatus(1);
            userRepository.save(maaUser);

            userSessionService.updateSessionIfPresent(maaUser.getUserId(), (session) -> {
                session.setMaaUser(maaUser);
            });
        });
    }

    /**
     * 更新用户信息
     *
     * @param userId    用户id
     * @param updateDTO 更新参数
     */
    public void updateUserInfo(@NotNull String userId, UserInfoUpdateDTO updateDTO) {
        userRepository.findById(userId).ifPresent((maaUser) -> {
            maaUser.updateAttribute(updateDTO);
            userRepository.save(maaUser);

            userSessionService.updateSessionIfPresent(maaUser.getUserId(), (s) -> {
                s.setMaaUser(maaUser);
            });
        });
    }

    /**
     * 为用户发送激活验证码
     *
     * @param userId 用户 id
     */
    public void sendActiveCodeByEmail(String userId) {
        userRepository.findById(userId).ifPresent((maaUser) -> {
            Assert.state(Objects.equals(maaUser.getStatus(), 0),
                    "用户已经激活，无法再次发送验证码");
            emailService.sendVCode(maaUser.getEmail());
        });
    }

    /**
     * 刷新token
     *
     * @param token token
     */
    public void refreshToken(String token) {
        // TODO 刷新JwtToken
    }

    /**
     * 通过邮箱激活码更新密码
     *
     * @param passwordResetDTO 通过邮箱修改密码请求
     */
    public void modifyPasswordByActiveCode(PasswordResetDTO passwordResetDTO) {
        emailService.verifyVCode(passwordResetDTO.getEmail(), passwordResetDTO.getActiveCode());
        var maaUser = userRepository.findByEmail(passwordResetDTO.getEmail());
        modifyPassword(maaUser.getUserId(), passwordResetDTO.getPassword());
    }

    /**
     * 根据邮箱校验用户是否存在
     *
     * @param email 用户邮箱
     */
    public void checkUserExistByEmail(String email) {
        if (null == userRepository.findByEmail(email)) {
            throw new MaaResultException(MaaStatusCode.MAA_USER_NOT_FOUND);
        }
    }

    /**
     * 激活账户
     *
     * @param activateDTO uuid
     */
    public void activateAccount(EmailActivateReq activateDTO) {
        String uuid = activateDTO.getNonce();
        String email = redisCache.getCache("UUID:" + uuid, String.class);
        Assert.notNull(email, "链接已过期");
        MaaUser user = userRepository.findByEmail(email);

        if (Objects.equals(user.getStatus(), 1)) {
            redisCache.removeCache("UUID:" + uuid);
            return;
        }
        // 激活账户
        user.setStatus(1);
        userRepository.save(user);

        updateLoginUserPermissions(user);
        // 清除缓存
        redisCache.removeCache("UUID:" + uuid);
    }

    /**
     * 实时更新用户权限(更新redis缓存中的用户权限)
     *
     * @param user 所依赖的数据库用户
     */
    private void updateLoginUserPermissions(MaaUser user) {
        var loginUser = userSessionService.getSession(user.getUserId());
        if (loginUser == null) return;

        userSessionService.updateSessionIfPresent(user.getUserId(), (s) -> {
            s.setPermissions(userDetailService.collectPermissionsFor(user));
        });
    }

}
