package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
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

    @GetMapping("activate")
    @Operation(summary = "获取活动用户", description = "通过token获得当前登录用户信息")
    public MaaResult<MaaUserInfo> activate(HttpServletRequest request) {
        //TODO
        return null;
    }

    @PostMapping("change")
    @Operation(summary = "修改用户信息", description = "通过id修改用户信息")
    public MaaResult<Void> change(@RequestBody @Valid MaaUserInfo userInfo) {
        //TODO
        return null;
    }

    @PutMapping("create")
    @Operation(summary = "创建新用户", description = "根据传入的用户创建新用户")
    public MaaResult<MaaUserInfo> create(@RequestBody MaaUser user) {
        return userService.addUser(user);
    }

    @DeleteMapping("delete/{user_id}")
    @Operation(summary = "删除用户", description = "将用户状态设置为删除")
    public MaaResult<Void> delete(@PathVariable("user_id") String id) {
        //TODO
        return null;
    }

    @GetMapping("info/{id}")
    @Operation(summary = "获取用户信息", description = "可变参数id，根据id获取用户信息")
    public MaaResult<MaaUserInfo> info(@PathVariable("id") String id) {
        return userService.findUserInfoById(id);
    }

    @GetMapping("query")
    public MaaResult<Void> query() {
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

    @PostMapping("register")
    public MaaResult<Void> register() {
        //TODO
        return null;
    }

    @PostMapping("activate/request")
    public MaaResult<Void> activateRequest() {
        //TODO
        return null;
    }

    @PostMapping("password/reset_request")
    public MaaResult<Void> passwordResetRequest() {
        //TODO
        return null;
    }

    @PostMapping("update/info")
    public MaaResult<Void> updateInfo() {
        //TODO
        return null;
    }

    @PostMapping("update/password")
    public MaaResult<Void> updatePassword() {
        //TODO
        return null;
    }

    @PostMapping("password/reset")
    public MaaResult<Void> passwordReset() {
        //TODO
        return null;
    }

    @PostMapping("login")
    @Operation(summary = "用户登录", description = "用户名与密码登录，成功后返回token")
    public MaaResult<Map<String, String>> login(@RequestBody @Valid LoginRequest user) {
        return userService.login(user);
    }
}
