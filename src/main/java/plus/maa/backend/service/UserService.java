package plus.maa.backend.service;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.lang.Assert;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTPayload;
import cn.hutool.jwt.JWTUtil;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.MaaStatusCode;
import plus.maa.backend.common.utils.converter.MaaUserConverter;
import plus.maa.backend.controller.request.*;
import plus.maa.backend.controller.response.MaaLoginRsp;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.controller.response.MaaUserInfo;
import plus.maa.backend.service.model.LoginUser;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.UserRepository;
import plus.maa.backend.repository.entity.MaaUser;

import java.time.LocalDateTime;
import java.util.*;

/**
 * @author AnselYuki
 */
@Setter
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private static final String REDIS_KEY_PREFIX_LOGIN = "LOGIN:";
    private final AuthenticationManager authenticationManager;
    private final RedisCache redisCache;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${maa-copilot.jwt.secret}")
    private String secret;
    @Value("${maa-copilot.jwt.expire}")
    private int expire;
    @Value("${maa-copilot.vcode.expire:600}")
    private int registrationCodeExpireInSecond;

    private LoginUser getLoginUserByToken(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        String redisKey = buildUserCacheKey(jwt.getPayload("userId").toString());
        return redisCache.getCache(redisKey, LoginUser.class);
    }

    /**
     * ????????????
     *
     * @param loginDTO ????????????
     * @return ?????????token????????????
     */
    public MaaResult<MaaLoginRsp> login(LoginDTO loginDTO) {
        // ?????? AuthenticationManager ?????? authenticate ??????????????????
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                loginDTO.getEmail(), loginDTO.getPassword());
        Authentication authenticate;
        authenticate = authenticationManager.authenticate(authenticationToken);
        // ????????????????????????????????????
        if (Objects.isNull(authenticate)) {
            throw new MaaResultException("????????????");
        }
        // ????????????????????????UserID????????????JwtToken,Token??????ResponseResult??????
        LoginUser principal = (LoginUser) authenticate.getPrincipal();
        String userId = String.valueOf(principal.getMaaUser().getUserId());
        String token = RandomStringUtils.random(16, true, true);
        DateTime now = DateTime.now();
        DateTime newTime = now.offsetNew(DateField.SECOND, expire);
        // ??????JwtToken??????????????????????????????????????????????????????????????????
        Map<String, Object> payload = new HashMap<>(4) {
            {
                put(JWTPayload.ISSUED_AT, now.getTime());
                put(JWTPayload.EXPIRES_AT, newTime.getTime());
                put(JWTPayload.NOT_BEFORE, now.getTime());
                put("userId", userId);
                put("token", token);
            }
        };

        // ??????????????????????????????Redis???UserID??????Key
        String cacheKey = buildUserCacheKey(userId);
        redisCache.updateCache(cacheKey, LoginUser.class, principal, cacheUser -> {
            String cacheToken = cacheUser.getToken();
            if (cacheToken != null && !"".equals(cacheToken)) {
                payload.put("token", cacheToken);
            } else {
                cacheUser.setToken(token);
            }
            return cacheUser;
        }, expire);

        String jwt = JWTUtil.createToken(payload, secret.getBytes());

        MaaLoginRsp rsp = new MaaLoginRsp();
        rsp.setToken(jwt);
        rsp.setValidAfter(LocalDateTime.now().toString());
        rsp.setValidBefore(newTime.toLocalDateTime().toString());
        rsp.setRefreshToken("");
        rsp.setRefreshTokenValidBefore("");
        rsp.setUserInfo(MaaUserConverter.INSTANCE.convert(principal.getMaaUser()));

        return MaaResult.success("????????????", rsp);
    }

    /**
     * ????????????
     *
     * @param loginUser ????????????
     * @param password  ?????????
     * @return ??????????????????
     */
    public MaaResult<Void> modifyPassword(LoginUser loginUser, String password) {
        MaaUser user = loginUser.getMaaUser();
        // ?????????????????????
        String newPassword = new BCryptPasswordEncoder().encode(password);
        user.setPassword(newPassword);
        userRepository.save(user);

        // ????????????jwt-token???????????????jwt
        String newJwtToken = RandomStringUtils.random(16, true, true);
        DateTime now = DateTime.now();
        DateTime newTime = now.offsetNew(DateField.SECOND, expire);
        Map<String, Object> payload = new HashMap<>(4) {
            {
                put(JWTPayload.ISSUED_AT, now.getTime());
                put(JWTPayload.EXPIRES_AT, newTime.getTime());
                put(JWTPayload.NOT_BEFORE, now.getTime());
                put("userId", user.getUserId());
                put("token", newJwtToken);
            }
        };
        String redisKey = buildUserCacheKey(user.getUserId());
        // ???????????????MaaUser?????????????????????..
        loginUser.setMaaUser(user);
        redisCache.updateCache(redisKey, LoginUser.class, loginUser, cacheUser -> {
            cacheUser.setToken(newJwtToken);
            return cacheUser;
        }, expire);

        String newJwt = JWTUtil.createToken(payload, secret.getBytes());
        // TODO ?????????????????????jwt

        return MaaResult.success(null);
    }

    /**
     * ????????????
     *
     * @param registerDTO ??????????????????
     * @return ?????????????????????????????????????????????
     */
    public MaaResult<MaaUserInfo> register(RegisterDTO registerDTO) {
        String encode = new BCryptPasswordEncoder().encode(registerDTO.getPassword());
        MaaUser user = new MaaUser();
        BeanUtils.copyProperties(registerDTO, user);
        user.setPassword(encode);
        user.setStatus(1);
        MaaUserInfo userInfo;
        if (!emailService.verifyVCode2(user.getEmail(), registerDTO.getRegistrationToken(),false)) {
            throw new MaaResultException(MaaStatusCode.MAA_REGISTRATION_CODE_NOT_FOUND);
        }
        try {
            MaaUser save = userRepository.save(user);
            userInfo = new MaaUserInfo(save);
        } catch (DuplicateKeyException e) {
            return MaaResult.fail(10001, "???????????????");
        }
        return MaaResult.success(userInfo);
    }

    /**
     * ???????????????JwtToken??????????????????????????????
     *
     * @param loginUser   ????????????
     * @param activateDTO ???????????????
     * @return ??????????????????
     */
    public MaaResult<Void> activateUser(LoginUser loginUser, ActivateDTO activateDTO) {
        if (Objects.equals(loginUser.getMaaUser().getStatus(), 1)) {
            return MaaResult.success();
        }
        String email = loginUser.getMaaUser().getEmail();
        emailService.verifyVCode(email, activateDTO.getToken());
        MaaUser user = loginUser.getMaaUser();
        user.setStatus(1);
        userRepository.save(user);
        updateLoginUserPermissions(1, user.getUserId());
        return MaaResult.success();
    }

    /**
     * ??????????????????
     *
     * @param loginUser ????????????
     * @param updateDTO ????????????
     * @return ????????????
     */
    public MaaResult<Void> updateUserInfo(LoginUser loginUser, UserInfoUpdateDTO updateDTO) {
        MaaUser user = loginUser.getMaaUser();
        user.updateAttribute(updateDTO);
        userRepository.save(user);
        redisCache.setCache(buildUserCacheKey(user.getUserId()), loginUser);
        return MaaResult.success(null);
    }

    /**
     * ?????????????????????????????????token?????????
     *
     * @param loginUser ????????????
     * @return ????????????
     */
    public MaaResult<Void> sendEmailCode(LoginUser loginUser) {
        Assert.state(Objects.equals(loginUser.getMaaUser().getStatus(), 0),
                "????????????????????????????????????????????????");
        String email = loginUser.getEmail();
        emailService.sendVCode(email);
        return MaaResult.success(null);
    }

    /**
     * ??????token
     *
     * @param token token
     * @return ????????????
     */
    public MaaResult<Void> refreshToken(String token) {
        // TODO ??????JwtToken
        return null;
    }

    /**
     * ?????????????????????????????????
     *
     * @param passwordResetDTO ??????????????????????????????
     * @return ????????????
     */
    public MaaResult<Void> modifyPasswordByActiveCode(PasswordResetDTO passwordResetDTO) {
        emailService.verifyVCode(passwordResetDTO.getEmail(), passwordResetDTO.getActiveCode());
        LoginUser loginUser = new LoginUser();
        MaaUser maaUser = userRepository.findByEmail(passwordResetDTO.getEmail());
        loginUser.setMaaUser(maaUser);
        return modifyPassword(loginUser, passwordResetDTO.getPassword());
    }

    /**
     * ????????????????????????????????????
     *
     * @param email ????????????
     */
    public void checkUserExistByEmail(String email) {
        if (null == userRepository.findByEmail(email)) {
            throw new MaaResultException(MaaStatusCode.MAA_USER_NOT_FOUND);
        }
    }

    /**
     * ????????????
     *
     * @param activateDTO uuid
     */
    public void activateAccount(EmailActivateReq activateDTO) {
        String uuid = activateDTO.getNonce();
        String email = redisCache.getCache("UUID:" + uuid, String.class);
        Assert.notNull(email, "???????????????");
        MaaUser user = userRepository.findByEmail(email);

        if (Objects.equals(user.getStatus(), 1)) {
            redisCache.removeCache("UUID:" + uuid);
            return;
        }
        // ????????????
        user.setStatus(1);
        userRepository.save(user);

        updateLoginUserPermissions(1, user.getUserId());
        // ????????????
        redisCache.removeCache("UUID:" + uuid);
    }

    /**
     * ????????????????????????(??????redis????????????????????????)
     *
     * @param permissions ?????????
     * @param userId      userId
     */
    private void updateLoginUserPermissions(int permissions, String userId) {
        LoginUser loginUser;
        // ??????????????? ????????????
        try {
            loginUser = (LoginUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        } catch (ClassCastException e) {
            return;
        }
        String cacheId = buildUserCacheKey(userId);

        redisCache.updateCache(cacheId, LoginUser.class, loginUser, cacheUser -> {
            Set<String> p = cacheUser.getPermissions();

            // ??????????????????
            cacheUser.getMaaUser().setStatus(permissions);
            for (int i = 0; i <= permissions; i++) {
                p.add(Integer.toString(i));
            }
            cacheUser.setPermissions(p);

            return cacheUser;
        }, expire);
    }

    private static String buildUserCacheKey(String userId) {
        return REDIS_KEY_PREFIX_LOGIN + userId;
    }
}
