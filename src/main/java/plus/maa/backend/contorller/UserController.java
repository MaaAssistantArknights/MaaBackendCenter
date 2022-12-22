package plus.maa.backend.contorller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import plus.maa.backend.domain.MaaResult;
import plus.maa.backend.service.LoginService;
import plus.maa.backend.vo.LoginVo;

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
    private final LoginService loginService;

    @PostMapping("login")
    @Operation(summary = "登录接口", description = "执行用户登录，登陆成功返回Token")
    public MaaResult<Map<String, String>> login(@RequestBody LoginVo user) {
        return loginService.login(user);
    }
}
