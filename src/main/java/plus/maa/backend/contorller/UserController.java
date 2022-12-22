package plus.maa.backend.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.domain.MaaResult;
import plus.maa.backend.model.MaaUser;
import plus.maa.backend.service.UserService;
import plus.maa.backend.vo.LoginVo;
import plus.maa.backend.vo.MaaUserInfo;

import java.util.Map;

/**
 * @author AnselYuki
 */
@Data
@Tag(name = "CopilotUser", description = "用户有关接口")
@RequestMapping("user")
@RestController
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @PostMapping("login")
    @Operation(summary = "登录接口", description = "执行用户登录，登陆成功返回Token")
    public MaaResult<Map<String, String>> login(@RequestBody LoginVo user) {
        return userService.login(user);
    }

    @GetMapping("info/{id}")
    public MaaResult<MaaUserInfo> getUserInfo(@PathVariable("id") String id) {
        return userService.findUserInfoById(id);
    }

    @PostMapping("create")
    public MaaResult<Void> createUser(@RequestBody MaaUser user) {
        return userService.addUser(user);
    }
}
