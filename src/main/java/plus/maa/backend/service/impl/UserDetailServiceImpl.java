package plus.maa.backend.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import plus.maa.backend.domain.LoginUser;

/**
 * @author AnselYuki
 */
@Slf4j
@Service
public class UserDetailServiceImpl implements UserDetailsService {
    /**
     * 查询用户信息
     *
     * @param username the username identifying the user whose data is required.
     * @return 用户详细信息
     * @throws UsernameNotFoundException 用户名未找到
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        //TODO 查询用户信息
        //数据封装成UserDetails返回
        return new LoginUser();
    }
}
