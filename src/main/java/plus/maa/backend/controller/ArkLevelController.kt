package plus.maa.backend.controller;

import java.util.List;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import plus.maa.backend.controller.response.copilot.ArkLevelInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.ArkLevelService;

/**
 * @author john180
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "ArkLevelController", description = "关卡数据管理接口")
public class ArkLevelController {
    private final ArkLevelService arkLevelService;

    @Operation(summary = "获取关卡数据")
    @ApiResponse(description = "关卡数据")
    @GetMapping("/arknights/level")
    public MaaResult<List<ArkLevelInfo>> getLevels() {
        return MaaResult.success(arkLevelService.getArkLevelInfos());
    }

}
