package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.controller.request.LoginRequest;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaUserInfo;
import plus.maa.backend.repository.entity.MaaUser;
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
    @Value("${maa-copilot.jwt.header}")
    private String header;

    /**
     * 获取当前登录的用户（通过请求携带的token）
     *
     * @param request http请求，获取头部携带的token
     * @return Activates a user account.
     */
    @GetMapping("activate")
    public MaaResult<MaaUserInfo> activate(HttpServletRequest request) {
        String token = request.getHeader(header);
        return userService.findActivateUser(token);
    }

    /**
     * Requests a new activation code.
     *
     * @return null
     */
    @PostMapping("/activate/request")
    public MaaResult<MaaUserInfo> activateRequest() {
        //TODO
        return null;
    }

    @PostMapping("update/password")
    public MaaResult<Void> updatePassword() {
        //TODO
        return null;
    }

    @PostMapping("update/info")
    public MaaResult<Void> updateInfo() {
        //TODO
        return null;
    }

    @PostMapping("password/reset")
    public MaaResult<Void> passwordReset() {
        //TODO
        return null;
    }

    @PostMapping("password/reset_request")
    public MaaResult<Void> passwordResetRequest() {
        //TODO
        return null;
    }

    /**
     * 刷新token
     *
     * @return null
     */
    @PostMapping("refresh")
    public MaaResult<Void> refresh() {
        //TODO
        return null;
    }

    /**
     * 用户注册
     *
     * @param user 传入用户参数
     * @return 注册成功用户信息摘要
     */
    @PostMapping("register")
    public MaaResult<MaaUserInfo> register(@RequestBody MaaUser user) {
        return userService.register(user);
    }

    @PostMapping("login")
    public MaaResult<Map<String, String>> login(@RequestBody @Valid LoginRequest user) {
        return userService.login(user);
    }
}
