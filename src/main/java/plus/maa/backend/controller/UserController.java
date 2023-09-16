package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.config.SpringDocConfig;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.config.security.AuthenticationHelper;
import plus.maa.backend.controller.request.user.*;
import plus.maa.backend.controller.response.user.MaaLoginRsp;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.user.MaaUserInfo;
import plus.maa.backend.service.EmailService;
import plus.maa.backend.service.UserService;

import java.io.IOException;

/**
 * 用户相关接口
 * <a href=
 * "https://github.com/MaaAssistantArknights/maa-copilot-frontend/blob/dev/src/apis/auth.ts">前端api约定文件</a>
 *
 * @author AnselYuki
 */
@Data
@Tag(name = "CopilotUser", description = "用户管理")
@RequestMapping("/user")
@Validated
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final EmailService emailService;
    private final MaaCopilotProperties properties;
    private final AuthenticationHelper helper;
    @Value("${maa-copilot.jwt.header}")
    private String header;

    /**
     * 更新当前用户的密码(根据原密码)
     *
     * @return http响应
     */
    @Operation(summary = "修改当前用户密码", description = "根据原密码")
    @ApiResponse(description = "修改密码结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/update/password")
    public MaaResult<Void> updatePassword(
            @Parameter(description = "修改密码请求") @RequestBody @Valid PasswordUpdateDTO updateDTO
    ) {
        userService.modifyPassword(helper.requireUserId(), updateDTO.getNewPassword());
        return MaaResult.success();
    }

    /**
     * 更新用户详细信息
     *
     * @param updateDTO 用户信息参数
     * @return http响应
     */
    @Operation(summary = "更新用户详细信息")
    @ApiResponse(description = "更新结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/update/info")
    public MaaResult<Void> updateInfo(
            @Parameter(description = "更新用户详细信息请求") @Valid @RequestBody UserInfoUpdateDTO updateDTO
    ) {
        userService.updateUserInfo(helper.requireUserId(), updateDTO);
        return MaaResult.success();
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
    public MaaResult<Void> passwordReset(@Parameter(description = "重置密码请求") @RequestBody @Valid PasswordResetDTO passwordResetDTO) {
        // 校验用户邮箱是否存在
        userService.checkUserExistByEmail(passwordResetDTO.getEmail());
        userService.modifyPasswordByActiveCode(passwordResetDTO);
        return MaaResult.success();
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
    public MaaResult<Void> passwordResetRequest(@Parameter(description = "发送重置密码的验证码请求") @RequestBody @Valid PasswordResetVCodeDTO passwordResetVCodeDTO) {
        // 校验用户邮箱是否存在
        userService.checkUserExistByEmail(passwordResetVCodeDTO.getEmail());
        emailService.sendVCode(passwordResetVCodeDTO.getEmail());
        return MaaResult.success();
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
    public MaaResult<MaaLoginRsp> refresh(@Parameter(description = "刷新token请求") @RequestBody RefreshReq request) {
        var res = userService.refreshToken(request.getRefreshToken());
        return MaaResult.success(res);
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
    public MaaResult<MaaUserInfo> register(@Parameter(description = "用户注册请求") @Valid @RequestBody RegisterDTO user) {
        return MaaResult.success(userService.register(user));
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
    public MaaResult<MaaLoginRsp> login(@Parameter(description = "登录请求") @RequestBody @Valid LoginDTO user) {
        return MaaResult.success("登陆成功", userService.login(user));
    }

    @PostMapping("/resendActivateUrl")
    @Operation(summary = "重新获取激活链接")
    @ApiResponse(description = "发送激活链接")
    public MaaResult<?> resendActivateUrl(@Parameter(description = "重新获取激活链接请求") @RequestBody @Valid ResendActivateUrlDTO activateUrlDTO) {
        userService.resendActivateUrl(activateUrlDTO);
        return MaaResult.success();
    }

    @GetMapping("/activateAccount")
    @Operation(summary = "激活账号")
    @ApiResponse(description = "激活账号结果")
    public MaaResult<Void> activateAccount(@Parameter(description = "激活请求") EmailActivateReq activateDTO,
                                           @Parameter(description = "页面跳转参数") HttpServletResponse response) {
        userService.activateAccount(activateDTO);
        // 激活成功 跳转页面
        try {
            response.sendRedirect(properties.getInfo().getFrontendDomain());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return MaaResult.success();
    }
}
