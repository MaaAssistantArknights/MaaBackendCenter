package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.info.GitProperties;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaSystemInfo;


/**
 * @author AnselYuki
 */
@Tag(name = "System", description = "系统管理接口")
@RequestMapping("")
@RestController
@RequiredArgsConstructor
public class SystemController {

    private final MaaCopilotProperties properties;
    @Nullable   // 非必须字段，当 Git 仓库不存在时为 null
    private final GitProperties gitProperties;

    @GetMapping("/")
    @Operation(summary = "Tests if the server is ready.")
    @ApiResponse(description = "系统启动信息")
    public MaaResult<String> test() {
        return MaaResult.success("Maa Copilot Server is Running", null);
    }

    @GetMapping("version")
    @Operation(summary = "Gets the current version of the server.")
    @ApiResponse(description = "系统版本信息")
    public MaaResult<MaaSystemInfo> getSystemVersion() {
        var systemInfo = new MaaSystemInfo();
        var info = properties.getInfo();
        systemInfo.setTitle(info.getTitle());
        systemInfo.setDescription(info.getDescription());
        systemInfo.setVersion(info.getVersion());
        systemInfo.setGit(gitProperties);
        return MaaResult.success(systemInfo);
    }

}
