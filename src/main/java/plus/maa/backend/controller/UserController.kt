package plus.maa.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import plus.maa.backend.config.doc.RequireJwt
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.config.security.AuthenticationHelper
import plus.maa.backend.controller.request.user.*
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.controller.response.user.MaaLoginRsp
import plus.maa.backend.controller.response.user.MaaUserInfo
import plus.maa.backend.service.EmailService
import plus.maa.backend.service.UserService

/**
 * 用户相关接口
 * [前端api约定文件](https://github.com/MaaAssistantArknights/maa-copilot-frontend/blob/dev/src/apis/auth.ts)
 *
 * @author AnselYuki
 */
@Tag(name = "CopilotUser", description = "用户管理")
@RequestMapping("/user")
@Validated
@RestController
class UserController(
    private val userService: UserService,
    private val emailService: EmailService,
    private val properties: MaaCopilotProperties,
    private val helper: AuthenticationHelper,
    @Value("\${maa-copilot.jwt.header}") private val header: String
) {

    /**
     * 更新当前用户的密码(根据原密码)
     *
     * @return http响应
     */
    @Operation(summary = "修改当前用户密码", description = "根据原密码")
    @ApiResponse(description = "修改密码结果")
    @RequireJwt
    @PostMapping("/update/password")
    fun updatePassword(
        @Parameter(description = "修改密码请求") @RequestBody updateDTO: @Valid PasswordUpdateDTO
    ): MaaResult<Void?> {
        userService.modifyPassword(helper.requireUserId(), updateDTO.newPassword)
        return success()
    }

    /**
     * 更新用户详细信息
     *
     * @param updateDTO 用户信息参数
     * @return http响应
     */
    @Operation(summary = "更新用户详细信息")
    @ApiResponse(description = "更新结果")
    @RequireJwt
    @PostMapping("/update/info")
    fun updateInfo(
        @Parameter(description = "更新用户详细信息请求") @RequestBody updateDTO: @Valid UserInfoUpdateDTO
    ): MaaResult<Void?> {
        userService.updateUserInfo(helper.requireUserId(), updateDTO)
        return success()
    }

    /**
     * 邮箱重设密码
     *
     * @param passwordResetDTO 通过邮箱修改密码请求
     * @return 成功响应
     */
    @PostMapping("/password/reset")
    @Operation(summary = "重置密码")
    @ApiResponse(description = "重置密码结果")
    fun passwordReset(@Parameter(description = "重置密码请求") @RequestBody passwordResetDTO: @Valid PasswordResetDTO?): MaaResult<Void?> {
        // 校验用户邮箱是否存在
        userService!!.checkUserExistByEmail(passwordResetDTO!!.email)
        userService.modifyPasswordByActiveCode(passwordResetDTO)
        return success()
    }

    /**
     * 验证码重置密码功能：
     * 发送验证码用于重置
     *
     * @return 成功响应
     */
    @PostMapping("/password/reset_request")
    @Operation(summary = "发送用于重置密码的验证码")
    @ApiResponse(description = "验证码发送结果")
    fun passwordResetRequest(@Parameter(description = "发送重置密码的验证码请求") @RequestBody passwordResetVCodeDTO: @Valid PasswordResetVCodeDTO): MaaResult<Void?> {
        // 校验用户邮箱是否存在
        userService.checkUserExistByEmail(passwordResetVCodeDTO.email)
        emailService.sendVCode(passwordResetVCodeDTO.email)
        return success()
    }

    /**
     * 刷新token
     *
     * @param request http请求，用于获取请求头
     * @return 成功响应
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新token")
    @ApiResponse(description = "刷新token结果")
    fun refresh(@Parameter(description = "刷新token请求") @RequestBody request: RefreshReq): MaaResult<MaaLoginRsp> {
        val res = userService.refreshToken(request.refreshToken)
        return success(res)
    }

    /**
     * 用户注册
     *
     * @param user 传入用户参数
     * @return 注册成功用户信息摘要
     */
    @PostMapping("/register")
    @Operation(summary = "用户注册")
    @ApiResponse(description = "注册结果")
    fun register(@Parameter(description = "用户注册请求") @RequestBody user: @Valid RegisterDTO): MaaResult<MaaUserInfo> {
        return success(userService.register(user))
    }

    /**
     * 获得注册时的验证码
     */
    @PostMapping("/sendRegistrationToken")
    @Operation(summary = "注册时发送验证码")
    @ApiResponse(description = "发送验证码结果", responseCode = "204")
    fun sendRegistrationToken(@Parameter(description = "发送注册验证码请求") @RequestBody regDTO: @Valid SendRegistrationTokenDTO): MaaResult<Void?> {
        userService.sendRegistrationToken(regDTO)
        return MaaResult(204, null, null)
    }

    /**
     * 用户登录
     *
     * @param user 登录参数
     * @return 成功响应，荷载JwtToken
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录")
    @ApiResponse(description = "登录结果")
    fun login(@Parameter(description = "登录请求") @RequestBody user: @Valid LoginDTO): MaaResult<MaaLoginRsp> {
        return success("登陆成功", userService.login(user))
    }
}
