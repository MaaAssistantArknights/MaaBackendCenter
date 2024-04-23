package plus.maa.backend.service

import org.springframework.dao.DuplicateKeyException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import plus.maa.backend.common.MaaStatusCode
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.controller.request.user.LoginDTO
import plus.maa.backend.controller.request.user.PasswordResetDTO
import plus.maa.backend.controller.request.user.RegisterDTO
import plus.maa.backend.controller.request.user.SendRegistrationTokenDTO
import plus.maa.backend.controller.request.user.UserInfoUpdateDTO
import plus.maa.backend.controller.response.MaaResultException
import plus.maa.backend.controller.response.user.MaaLoginRsp
import plus.maa.backend.controller.response.user.MaaUserInfo
import plus.maa.backend.repository.UserRepository
import plus.maa.backend.repository.entity.MaaUser
import plus.maa.backend.service.jwt.JwtExpiredException
import plus.maa.backend.service.jwt.JwtInvalidException
import plus.maa.backend.service.jwt.JwtService
import java.util.UUID

/**
 * @author AnselYuki
 */
@Service
class UserService(
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val passwordEncoder: PasswordEncoder,
    private val userDetailService: UserDetailServiceImpl,
    private val jwtService: JwtService,
    private val properties: MaaCopilotProperties,
) {
    /**
     * 登录方法
     *
     * @param loginDTO 登录参数
     * @return 携带了token的封装类
     */
    fun login(loginDTO: LoginDTO): MaaLoginRsp {
        val user = userRepository.findByEmail(loginDTO.email)
        if (user == null || !passwordEncoder.matches(loginDTO.password, user.password)) {
            throw MaaResultException(401, "用户不存在或者密码错误")
        }
        // 未激活的用户
        if (user.status == 0) {
            throw MaaResultException(MaaStatusCode.MAA_USER_NOT_ENABLED)
        }

        val jwtId = UUID.randomUUID().toString()
        val jwtIds = user.refreshJwtIds
        jwtIds.add(jwtId)
        while (jwtIds.size > properties.jwt.maxLogin) jwtIds.removeAt(0)
        userRepository.save(user)

        val authorities = userDetailService.collectAuthoritiesFor(user)
        val authToken = jwtService.issueAuthToken(user.userId!!, null, authorities)
        val refreshToken = jwtService.issueRefreshToken(user.userId, jwtId)

        return MaaLoginRsp(
            authToken.value,
            authToken.expiresAt,
            authToken.notBefore,
            refreshToken.value,
            refreshToken.expiresAt,
            refreshToken.notBefore,
            MaaUserInfo(user),
        )
    }

    /**
     * 修改密码
     *
     * @param userId      当前用户
     * @param rawPassword 新密码
     */
    fun modifyPassword(userId: String, rawPassword: String, originPassword: String? = null, verifyOriginPassword: Boolean = true) {
        val maaUser = userRepository.findByIdOrNull(userId) ?: return
        if (verifyOriginPassword) {
            check(!originPassword.isNullOrEmpty()) {
                "请输入原密码"
            }
            check(passwordEncoder.matches(originPassword, maaUser.password)) {
                "原密码错误"
            }
        }
        // 修改密码的逻辑，应当使用与 authentication provider 一致的编码器
        maaUser.password = passwordEncoder.encode(rawPassword)
        // 更新密码时，如果用户未启用则自动启用
        if (maaUser.status == 0) {
            maaUser.status = 1
        }
        maaUser.refreshJwtIds = ArrayList()
        userRepository.save(maaUser)
    }

    /**
     * 用户注册
     *
     * @param registerDTO 传入用户参数
     * @return 返回注册成功的用户摘要（脱敏）
     */
    fun register(registerDTO: RegisterDTO): MaaUserInfo {
        // 校验验证码
        emailService.verifyVCode(registerDTO.email, registerDTO.registrationToken)

        val encoded = passwordEncoder.encode(registerDTO.password)

        val user = MaaUser(
            userName = registerDTO.userName,
            email = registerDTO.email,
            password = encoded,
            status = 1,
        )
        return try {
            userRepository.save(user).run(::MaaUserInfo)
        } catch (e: DuplicateKeyException) {
            throw MaaResultException(MaaStatusCode.MAA_USER_EXISTS)
        }
    }

    /**
     * 更新用户信息
     *
     * @param userId    用户id
     * @param updateDTO 更新参数
     */
    fun updateUserInfo(userId: String, updateDTO: UserInfoUpdateDTO) {
        val maaUser = userRepository.findByIdOrNull(userId) ?: return
        maaUser.userName = updateDTO.userName
        userRepository.save(maaUser)
    }

    /**
     * 刷新token
     *
     * @param token token
     */
    fun refreshToken(token: String): MaaLoginRsp {
        try {
            val old = jwtService.verifyAndParseRefreshToken(token)

            val userId = old.subject
            val user = userRepository.findById(userId).orElseThrow()

            val refreshJwtIds = user.refreshJwtIds
            val idIndex = refreshJwtIds.indexOf(old.jwtId)
            if (idIndex < 0) throw MaaResultException(401, "invalid token")

            val jwtId = UUID.randomUUID().toString()
            refreshJwtIds[idIndex] = jwtId

            userRepository.save(user)

            val refreshToken = jwtService.newRefreshToken(old, jwtId)

            val authorities = userDetailService.collectAuthoritiesFor(user)
            val authToken = jwtService.issueAuthToken(userId, null, authorities)

            return MaaLoginRsp(
                authToken.value,
                authToken.expiresAt,
                authToken.notBefore,
                refreshToken.value,
                refreshToken.expiresAt,
                refreshToken.notBefore,
                MaaUserInfo(user),
            )
        } catch (e: JwtInvalidException) {
            throw MaaResultException(401, e.message)
        } catch (e: JwtExpiredException) {
            throw MaaResultException(401, e.message)
        } catch (e: NoSuchElementException) {
            throw MaaResultException(401, e.message)
        }
    }

    /**
     * 通过邮箱激活码更新密码
     *
     * @param passwordResetDTO 通过邮箱修改密码请求
     */
    fun modifyPasswordByActiveCode(passwordResetDTO: PasswordResetDTO) {
        emailService.verifyVCode(passwordResetDTO.email, passwordResetDTO.activeCode)
        val maaUser = userRepository.findByEmail(passwordResetDTO.email)
        modifyPassword(maaUser!!.userId!!, passwordResetDTO.password, verifyOriginPassword = false)
    }

    /**
     * 根据邮箱校验用户是否存在
     *
     * @param email 用户邮箱
     */
    fun checkUserExistByEmail(email: String) {
        if (null == userRepository.findByEmail(email)) {
            throw MaaResultException(MaaStatusCode.MAA_USER_NOT_FOUND)
        }
    }

    /**
     * 注册时发送验证码
     */
    fun sendRegistrationToken(regDTO: SendRegistrationTokenDTO) {
        // 判断用户是否存在
        val maaUser = userRepository.findByEmail(regDTO.email)
        if (maaUser != null) {
            // 用户已存在
            throw MaaResultException(MaaStatusCode.MAA_USER_EXISTS)
        }
        // 发送验证码
        emailService.sendVCode(regDTO.email)
    }

    fun findByUserIdOrDefault(id: String) = userRepository.findByUserId(id) ?: MaaUser.UNKNOWN

    fun findByUsersId(ids: Iterable<String>): UserDict {
        return userRepository.findAllById(ids).let(::UserDict)
    }

    class UserDict(users: List<MaaUser>) {
        private val userMap = users.associateBy { it.userId }
        operator fun get(id: String): MaaUser? = userMap[id]
        fun getOrDefault(id: String) = get(id) ?: MaaUser.UNKNOWN
    }
}
