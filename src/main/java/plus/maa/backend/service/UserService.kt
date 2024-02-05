package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.MaaStatusCode;
import plus.maa.backend.common.utils.converter.MaaUserConverter;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.controller.request.user.*;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.controller.response.user.MaaLoginRsp;
import plus.maa.backend.controller.response.user.MaaUserInfo;
import plus.maa.backend.repository.UserRepository;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.jwt.JwtExpiredException;
import plus.maa.backend.service.jwt.JwtInvalidException;
import plus.maa.backend.service.jwt.JwtService;

import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

/**
 * @author AnselYuki
 */
@Setter
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final UserDetailServiceImpl userDetailService;
    private final JwtService jwtService;
    private final MaaUserConverter maaUserConverter;
    private final MaaCopilotProperties properties;

    /**
     * 登录方法
     *
     * @param loginDTO 登录参数
     * @return 携带了token的封装类
     */
    public MaaLoginRsp login(LoginDTO loginDTO) {
        var user = userRepository.findByEmail(loginDTO.getEmail());
        if (user == null || !passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())) {
            throw new MaaResultException(401, "用户不存在或者密码错误");
        }
        // 未激活的用户
        if (Objects.equals(user.getStatus(), 0)) {
            throw new MaaResultException(MaaStatusCode.MAA_USER_NOT_ENABLED);
        }

        var jwtId = UUID.randomUUID().toString();
        var jwtIds = user.getRefreshJwtIds();
        jwtIds.add(jwtId);
        while (jwtIds.size() > properties.getJwt().getMaxLogin()) jwtIds.remove(0);
        userRepository.save(user);

        var authorities = userDetailService.collectAuthoritiesFor(user);
        var authToken = jwtService.issueAuthToken(user.getUserId(), null, authorities);
        var refreshToken = jwtService.issueRefreshToken(user.getUserId(), jwtId);

        return new MaaLoginRsp(
                authToken.getValue(),
                authToken.getExpiresAt(),
                authToken.getNotBefore(),
                refreshToken.getValue(),
                refreshToken.getExpiresAt(),
                refreshToken.getNotBefore(),
                maaUserConverter.convert(user)
        );
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
        maaUser.setRefreshJwtIds(new ArrayList<>());
        userRepository.save(maaUser);
    }

    /**
     * 用户注册
     *
     * @param registerDTO 传入用户参数
     * @return 返回注册成功的用户摘要（脱敏）
     */
    public MaaUserInfo register(RegisterDTO registerDTO) {
        String encode = passwordEncoder.encode(registerDTO.getPassword());

        // 校验验证码
        emailService.verifyVCode(registerDTO.getEmail(), registerDTO.getRegistrationToken());

        MaaUser user = new MaaUser();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(encode);
        user.setStatus(1);
        MaaUserInfo userInfo;
        try {
            MaaUser save = userRepository.save(user);
            userInfo = new MaaUserInfo(save);
        } catch (DuplicateKeyException e) {
            throw new MaaResultException(MaaStatusCode.MAA_USER_EXISTS);
        }
        return userInfo;
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
        });
    }

    /**
     * 刷新token
     *
     * @param token token
     */
    public MaaLoginRsp refreshToken(String token) {
        try {
            var old = jwtService.verifyAndParseRefreshToken(token);

            var userId = old.getSubject();
            var user = userRepository.findById(userId).orElseThrow();

            var refreshJwtIds = user.getRefreshJwtIds();
            int idIndex = refreshJwtIds.indexOf(old.getJwtId());
            if (idIndex < 0) throw new MaaResultException(401, "invalid token");

            var jwtId = UUID.randomUUID().toString();
            refreshJwtIds.set(idIndex, jwtId);

            userRepository.save(user);

            var refreshToken = jwtService.newRefreshToken(old, jwtId);

            var authorities = userDetailService.collectAuthoritiesFor(user);
            var authToken = jwtService.issueAuthToken(userId, null, authorities);

            return new MaaLoginRsp(
                    authToken.getValue(),
                    authToken.getExpiresAt(),
                    authToken.getNotBefore(),
                    refreshToken.getValue(),
                    refreshToken.getExpiresAt(),
                    refreshToken.getNotBefore(),
                    maaUserConverter.convert(user)
            );
        } catch (JwtInvalidException | JwtExpiredException | NoSuchElementException e) {
            throw new MaaResultException(401, e.getMessage());
        }
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
     * 注册时发送验证码
     */
    public void sendRegistrationToken(SendRegistrationTokenDTO regDTO) {
        // 判断用户是否存在
        MaaUser maaUser = userRepository.findByEmail(regDTO.getEmail());
        if (maaUser != null) {
            // 用户已存在
            throw new MaaResultException(MaaStatusCode.MAA_USER_EXISTS);
        }
        // 发送验证码
        emailService.sendVCode(regDTO.getEmail());
    }

}
