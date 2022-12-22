package plus.maa.backend.service;

import plus.maa.backend.domain.MaaResult;
import plus.maa.backend.model.MaaUser;
import plus.maa.backend.vo.LoginVo;
import plus.maa.backend.vo.MaaUserInfo;

import java.util.Map;

/**
 * @author AnselYuki
 */
public interface UserService {
    /**
     * 用户登录
     *
     * @param user 用户对象
     * @return 登录响应
     */
    MaaResult<Map<String, String>> login(LoginVo user);

    /**
     * 通过id获取用户信息
     *
     * @param id 用户id
     * @return 用户信息封装
     */
    MaaResult<MaaUserInfo> findUserInfoById(String id);

    /**
     * 创建新用户
     *
     * @param user Maa用户实体类
     * @return 创建响应
     */
    MaaResult<Void> addUser(MaaUser user);
}
