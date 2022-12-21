package plus.maa.backend.contorller;

import plus.maa.backend.domain.ResponseResult;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author AnselYuki
 */
@RestController
public class HelloController {
    @RequestMapping(value = "/hello", method = {RequestMethod.POST, RequestMethod.GET})
    @PreAuthorize("hasAnyAuthority('hello')")
    @Operation(summary = "测试用接口")
    public ResponseResult<String> hello() {
        return new ResponseResult<>(200, "OK", "Hello Spring Security");
    }
}
