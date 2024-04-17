package plus.maa.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.controller.response.copilot.ArkLevelInfo
import plus.maa.backend.service.level.ArkLevelService

/**
 * @author john180
 */
@RestController
@Tag(name = "ArkLevelController", description = "关卡数据管理接口")
class ArkLevelController(
    private val arkLevelService: ArkLevelService,
) {
    @GetMapping("/arknights/level")
    @ApiResponse(description = "关卡数据")
    @Operation(summary = "获取关卡数据")
    fun getLevels(): MaaResult<List<ArkLevelInfo>> = success(arkLevelService.arkLevelInfos)
}
