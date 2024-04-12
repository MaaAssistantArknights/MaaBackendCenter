package plus.maa.backend.service.model

import com.fasterxml.jackson.annotation.JsonIgnore
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import plus.maa.backend.repository.entity.MaaUser

/**
 * @author AnselYuki
 */
class LoginUser(
    private val maaUser: MaaUser,
    private val authorities: Collection<GrantedAuthority?>,
) : UserDetails {
    @JsonIgnore
    override fun getAuthorities(): Collection<GrantedAuthority?> = authorities

    @JsonIgnore
    override fun getPassword(): String = maaUser.password

    val userId: String?
        get() = maaUser.userId

    /**
     * Spring Security框架中的username即唯一身份标识（ID）
     * 效果同getEmail
     *
     * @return 用户邮箱
     */
    @JsonIgnore
    override fun getUsername(): String = maaUser.email

    @get:JsonIgnore
    val email: String
        get() = maaUser.email

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = true

    override fun isCredentialsNonExpired(): Boolean = true

    /**
     * 默认用户为0(禁用)，1为启用
     *
     * @return 账户启用状态
     */
    override fun isEnabled(): Boolean = true
}
