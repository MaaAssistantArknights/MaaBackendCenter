package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import plus.maa.backend.common.annotation.CurrentUser;
import plus.maa.backend.controller.request.*;
import plus.maa.backend.controller.response.MaaLoginRsp;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaUserInfo;
import plus.maa.backend.service.EmailService;
import plus.maa.backend.service.UserService;
import plus.maa.backend.service.model.LoginUser;

/**
 * 用户相关接口
 * <a href="https://github.com/MaaAssistantArknights/maa-copilot-frontend/blob/dev/src/apis/auth.ts">前端api约定文件</a>
 *
 * @author AnselYuki
 */
@Data
@Slf4j
@Tag(name = "CopilotUser")
@RequestMapping("/user")
@Validated
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final EmailService emailService;
    @Value("${maa-copilot.jwt.header}")
    private String header;

    /**
     * 激活token中的用户
     *
     * @param activateDTO 激活码
     * @return 成功响应
     */
    @PostMapping("/activate")
    public MaaResult<Void> activate(@CurrentUser LoginUser user,
                                    @Valid @RequestBody ActivateDTO activateDTO) {
        return userService.activateUser(user, activateDTO);
    }

    /**
     * 注册完成后发送邮箱激活码
     *
     * @return null
     */
    @PostMapping("/activate/request")
    public MaaResult<Void> activateRequest(@CurrentUser LoginUser user) {
        return userService.senEmailCode(user);
    }

    /**
     * 更新当前用户的密码(根据原密码)
     *
     * @return http响应
     */
    @PostMapping("/update/password")
    public MaaResult<Void> updatePassword(@CurrentUser LoginUser user,
                                          @RequestBody @Valid PasswordUpdateDTO updateDTO) {
        return userService.modifyPassword(user, updateDTO.getNewPassword());
    }

    /**
     * 更新用户详细信息
     *
     * @param updateDTO 用户信息参数
     * @return http响应
     */
    @PostMapping("/update/info")
    public MaaResult<Void> updateInfo(@CurrentUser LoginUser user,
                                      @Valid @RequestBody UserInfoUpdateDTO updateDTO) {
        return userService.updateUserInfo(user, updateDTO);
    }

    //TODO 邮件重置密码需要在用户未登录的情况下使用，需要修改

    /**
     * 邮箱重设密码
     *
     * @param request http响应
     * @return 成功响应
     */
    @PostMapping("/password/reset")
    public MaaResult<Void> passwordReset(@CurrentUser LoginUser user, String password, HttpServletRequest request) {
        String jwtToken = request.getHeader(header);
        return userService.modifyPasswordByActiveCode(user, password, jwtToken);
    }

    /**
     * 验证码重置密码功能：
     * 发送验证码用于重置
     *
     * @return 成功响应
     */
    @PostMapping("/password/reset_request")
    public MaaResult<Void> passwordResetRequest(String email) {
        emailService.sendVCode(email);
        return MaaResult.success(null);
    }

    /**
     * 刷新token
     *
     * @param request http请求，用于获取请求头
     * @return 成功响应
     */
    @PostMapping("/refresh")
    public MaaResult<Void> refresh(HttpServletRequest request) {
        String token = request.getHeader(header);
        return userService.refreshToken(token);
    }

    /**
     * 用户注册
     *
     * @param user 传入用户参数
     * @return 注册成功用户信息摘要
     */
    @PostMapping("/register")
    public MaaResult<MaaUserInfo> register(@Valid @RequestBody RegisterDTO user) {
        return userService.register(user);
    }

    /**
     * 用户登录
     *
     * @param user 登录参数
     * @return 成功响应，荷载JwtToken
     */
    @PostMapping("/login")
    public MaaResult<MaaLoginRsp> login(@RequestBody @Valid LoginDTO user) {
        return userService.login(user);
    }
}
