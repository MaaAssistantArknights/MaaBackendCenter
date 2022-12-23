package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import plus.maa.backend.controller.response.MaaResult;

/**
 * @author AnselYuki
 */
@Data
@Tag(name = "System", description = "系统接口")
@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class SystemController {
    @GetMapping("/")
    public MaaResult<String> test() {
        return MaaResult.success("Maa Copilot Server is Running", null);
    }
}
