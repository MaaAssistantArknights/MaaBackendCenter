package plus.maa.backend.service;

import plus.maa.backend.domain.ResponseResult;
import plus.maa.backend.vo.LoginVo;

import java.util.Map;

/**
 * @author AnselYuki
 */
public interface LoginService {
    /**
     * 用户登录
     *
     * @param user 用户对象
     * @return 登录响应
     */
    ResponseResult<Map<String, String>> login(LoginVo user);

    /**
     * 退出登录，将传入jwt载荷区的用户执行删除
     *
     * @param token JwtToken
     * @return Http响应
     */
    ResponseResult<?> loginOut(String token);
}
