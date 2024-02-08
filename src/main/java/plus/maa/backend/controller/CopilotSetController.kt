package plus.maa.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import lombok.RequiredArgsConstructor
import org.springframework.web.bind.annotation.*
import plus.maa.backend.config.doc.RequireJwt
import plus.maa.backend.config.security.AuthenticationHelper
import plus.maa.backend.controller.request.CommonIdReq
import plus.maa.backend.controller.request.CopilotSetQuery
import plus.maa.backend.controller.request.CopilotSetUpdateReq
import plus.maa.backend.controller.request.copilotset.CopilotSetCreateReq
import plus.maa.backend.controller.request.copilotset.CopilotSetModCopilotsReq
import plus.maa.backend.controller.response.CopilotSetPageRes
import plus.maa.backend.controller.response.CopilotSetRes
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.service.CopilotSetService

/**
 * @author dragove
 * create on 2024-01-01
 */
@Tag(name = "CopilotSet", description = "作业集相关接口")
@RequestMapping("/set")
@RestController
@RequiredArgsConstructor
class CopilotSetController(
    private val service: CopilotSetService,
    private val helper: AuthenticationHelper
) {

    @Operation(summary = "查询作业集列表")
    @ApiResponse(description = "作业集id")
    @PostMapping("/query")
    fun querySets(
        @Parameter(description = "作业集列表查询请求") @RequestBody req: @Valid CopilotSetQuery
    ): MaaResult<CopilotSetPageRes> {
        return success(service.query(req))
    }

    @Operation(summary = "查询作业集列表")
    @ApiResponse(description = "作业集id")
    @GetMapping("/get")
    fun getSet(@RequestParam @Parameter(description = "作业id") id: Long): MaaResult<CopilotSetRes> {
        return success(service.get(id))
    }

    @Operation(summary = "创建作业集")
    @ApiResponse(description = "作业集id")
    @RequireJwt
    @PostMapping("/create")
    fun createSet(
        @Parameter(description = "作业集新增请求") @RequestBody req: @Valid CopilotSetCreateReq
    ): MaaResult<Long> {
        return success(service.create(req, helper.userId))
    }

    @Operation(summary = "添加作业集作业列表")
    @RequireJwt
    @PostMapping("/add")
    fun addCopilotIds(
        @Parameter(description = "作业集中加入新作业请求") @RequestBody req: @Valid CopilotSetModCopilotsReq
    ): MaaResult<Void?> {
        service.addCopilotIds(req, helper.userId!!)
        return success()
    }

    @Operation(summary = "添加作业集作业列表")
    @RequireJwt
    @PostMapping("/remove")
    fun removeCopilotIds(
        @Parameter(description = "作业集中删除作业请求") @RequestBody req: @Valid CopilotSetModCopilotsReq
    ): MaaResult<Void?> {
        service.removeCopilotIds(req, helper.userId!!)
        return success()
    }

    @Operation(summary = "更新作业集信息")
    @RequireJwt
    @PostMapping("/update")
    fun updateCopilotSet(
        @Parameter(description = "更新作业集信息请求") @RequestBody req: @Valid CopilotSetUpdateReq
    ): MaaResult<Void?> {
        service.update(req, helper.userId!!)
        return success()
    }

    @Operation(summary = "删除作业集")
    @RequireJwt
    @PostMapping("/delete")
    fun deleteCopilotSet(
        @Parameter(description = "删除作业集信息请求") @RequestBody req: @Valid CommonIdReq<Long>
    ): MaaResult<Void?> {
        service.delete(req.id!!, helper.userId!!)
        return success()
    }
}