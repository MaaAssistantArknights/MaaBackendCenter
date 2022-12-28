package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.controller.request.LoginDTO;
import plus.maa.backend.controller.request.PasswordUpdateDTO;
import plus.maa.backend.controller.request.RegisterDTO;
import plus.maa.backend.controller.request.UserInfoUpdateDTO;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaUserInfo;
import plus.maa.backend.service.EmailService;
import plus.maa.backend.service.UserService;

import java.util.Map;

/**
 * 用户相关接口
 * <a href="https://github.com/MaaAssistantArknights/maa-copilot-frontend/blob/dev/src/apis/auth.ts">前端api约定文件</a>
 *
 * @author AnselYuki
 */
@Data
@Slf4j
@Tag(name = "CopilotUser")
@RequestMapping("user")
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
     * @param token 激活码
     * @return 成功响应
     */
    @GetMapping("activate")
    public MaaResult<Void> activate(String token, HttpServletRequest request) {
        String jwtToken = request.getHeader(header);
        return userService.activateUser(token, jwtToken);
    }

    /**
     * 注册完成后发送邮箱激活码
     *
     * @param request http请求，用于获取token
     * @return null
     */
    @PostMapping("/activate/request")
    public MaaResult<Void> activateRequest(HttpServletRequest request) {
        String token = request.getHeader(header);
        return userService.senEmailCode(token);
    }

    /**
     * 更新当前用户的密码(根据原密码)
     *
     * @return http响应
     */
    @PostMapping("update/password")
    public MaaResult<Void> updatePassword(@RequestBody @Valid PasswordUpdateDTO updateDTO, HttpServletRequest request) {
        String token = request.getHeader(header);
        return userService.modifyPassword(token, updateDTO.getNewPassword());
    }

    /**
     * 更新用户详细信息
     *
     * @param updateDTO 用户信息参数
     * @return http响应
     */
    @PostMapping("update/info")
    public MaaResult<Void> updateInfo(@RequestBody UserInfoUpdateDTO updateDTO, HttpServletRequest request) {
        String token = request.getHeader(header);
        return userService.updateUserInfo(token, updateDTO);
    }

    /**
     * 邮箱重设密码
     *
     * @param token   邮箱激活码
     * @param request http响应
     * @return 成功响应
     */
    @PostMapping("password/reset")
    public MaaResult<Void> passwordReset(String token, String password, HttpServletRequest request) {
        String jwtToken = request.getHeader(header);
        return userService.modifyPasswordByActiveCode(token, password, jwtToken);
    }

    /**
     * 验证码重置密码功能：
     * 发送验证码用于重置
     *
     * @return 成功响应
     */
    @PostMapping("password/reset_request")
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
    @PostMapping("refresh")
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
    @PostMapping("register")
    public MaaResult<MaaUserInfo> register(@RequestBody RegisterDTO user) {
        return userService.register(user);
    }

    /**
     * 用户登录
     *
     * @param user 登录参数
     * @return 成功响应，荷载JwtToken
     */
    @PostMapping("login")
    public MaaResult<Map<String, String>> login(@RequestBody @Valid LoginDTO user) {
        return userService.login(user);
    }
}
