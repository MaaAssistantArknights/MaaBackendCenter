package plus.maa.backend.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import plus.maa.backend.repository.entity.MaaUser;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author AnselYuki
 */
@AllArgsConstructor
public class LoginUser implements UserDetails {

    private MaaUser maaUser;
    private List<SimpleGrantedAuthority> authorities;

    public LoginUser(MaaUser maaUser, Set<String> permissions) {
        this.maaUser = maaUser;
        this.authorities = permissions.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());
    }

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return maaUser.getPassword();
    }

    public String getUserId() {
        return maaUser.getUserId();
    }

    /**
     * Spring Security框架中的username即唯一身份标识（ID）
     * 效果同getEmail
     *
     * @return 用户邮箱
     */
    @Override
    @JsonIgnore
    public String getUsername() {
        return maaUser.getEmail();
    }

    @JsonIgnore
    public String getEmail() {
        return maaUser.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * 默认用户为0(禁用)，1为启用
     *
     * @return 账户启用状态
     */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
