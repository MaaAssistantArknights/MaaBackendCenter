package plus.maa.backend.contorller;

import plus.maa.backend.domain.ResponseResult;
import plus.maa.backend.service.LoginService;
import plus.maa.backend.vo.LoginVo;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author AnselYuki
 */
@Data
@RestController
public class LoginController {
    private final LoginService loginService;
    @Value("${anselyuki.jwt.header}")
    private String header;

    @Autowired
    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/user/login")
    @Operation(summary = "登录接口", description = "执行用户登录，登陆成功返回Token")
    public ResponseResult<Map<String, String>> login(@RequestBody LoginVo user) {
        return loginService.login(user);
    }

    @RequestMapping(value = "/user/loginOut", method = {RequestMethod.POST, RequestMethod.GET})
    @Operation(summary = "登出接口", description = "执行用户登出，将Redis中登录成功的用户删除")
    public ResponseResult<?> loginOut(HttpServletRequest request) {
        String jwtToken = request.getHeader(header);
        return loginService.loginOut(jwtToken);
    }
}
