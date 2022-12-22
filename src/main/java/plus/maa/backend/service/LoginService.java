package plus.maa.backend.service;

import plus.maa.backend.domain.MaaResult;
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
    MaaResult<Map<String, String>> login(LoginVo user);
}
