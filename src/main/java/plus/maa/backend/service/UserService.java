package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.utils.converter.MaaUserConverter;
import plus.maa.backend.controller.request.user.UserInfoUpdateDTO;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.controller.response.user.MaaLoginRsp;
import plus.maa.backend.repository.UserRepository;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.jwt.JwtExpiredException;
import plus.maa.backend.service.jwt.JwtInvalidException;
import plus.maa.backend.service.jwt.JwtService;

import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * @author AnselYuki
 */
@Setter
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    // 未来转为配置项
    private static final int LOGIN_LIMIT = 1;

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final MaaUserConverter maaUserConverter;

    /**
     * 授权登录 签发本地服务器令牌
     *
     * @param  maaUser MAA Account 邮箱/用户名
     * @return 携带了token的封装类
     */
    public MaaLoginRsp login(MaaUser maaUser) {
        var userOptional = userRepository.findByEmail(maaUser.getEmail());
        if (userOptional.isEmpty()) {
            throw new RuntimeException(":(");
        }

        var user = userOptional.get();
        var jwtId = UUID.randomUUID().toString();
        var jwtIds = user.getRefreshJwtIds();
        jwtIds.add(jwtId);
        while (jwtIds.size() > LOGIN_LIMIT) jwtIds.remove(0);
        userRepository.save(user);

        var authorities = UserDetailServiceImpl.collectAuthoritiesFor(user);
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

            var authorities = UserDetailServiceImpl.collectAuthoritiesFor(user);
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






}
