package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.config.doc.RequireJwt;
import plus.maa.backend.config.security.AuthenticationHelper;
import plus.maa.backend.controller.request.CommonIdReq;
import plus.maa.backend.controller.request.CopilotSetQuery;
import plus.maa.backend.controller.request.CopilotSetUpdateReq;
import plus.maa.backend.controller.request.copilotset.CopilotSetCreateReq;
import plus.maa.backend.controller.request.copilotset.CopilotSetModCopilotsReq;
import plus.maa.backend.controller.response.CopilotSetPageRes;
import plus.maa.backend.controller.response.CopilotSetRes;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.CopilotSetService;

/**
 * @author dragove
 * create on 2024-01-01
 */
@Tag(name = "CopilotSet", description = "作业集相关接口")
@RequestMapping("/set")
@RestController
@RequiredArgsConstructor
public class CopilotSetController {

    private final CopilotSetService service;
    private final AuthenticationHelper helper;

    @Operation(summary = "查询作业集列表")
    @ApiResponse(description = "作业集id")
    @PostMapping("/query")
    public MaaResult<CopilotSetPageRes> querySets(
            @Parameter(description = "作业集列表查询请求") @Valid @RequestBody CopilotSetQuery req) {
        return MaaResult.success(service.query(req));
    }

    @Operation(summary = "查询作业集列表")
    @ApiResponse(description = "作业集id")
    @GetMapping("/get")
    public MaaResult<CopilotSetRes> getSet(@RequestParam @Parameter(description = "作业id") long id) {
        return MaaResult.success(service.get(id));
    }


    @Operation(summary = "创建作业集")
    @ApiResponse(description = "作业集id")
    @RequireJwt
    @PostMapping("/create")
    public MaaResult<Long> createSet(
            @Parameter(description = "作业集新增请求") @Valid @RequestBody CopilotSetCreateReq req) {
        return MaaResult.success(service.create(req, helper.getUserId()));
    }

    @Operation(summary = "添加作业集作业列表")
    @RequireJwt
    @PostMapping("/add")
    public MaaResult<Void> addCopilotIds(
            @Parameter(description = "作业集中加入新作业请求") @Valid @RequestBody CopilotSetModCopilotsReq req) {
        service.addCopilotIds(req, helper.getUserId());
        return MaaResult.success();
    }

    @Operation(summary = "添加作业集作业列表")
    @RequireJwt
    @PostMapping("/remove")
    public MaaResult<Void> removeCopilotIds(
            @Parameter(description = "作业集中删除作业请求") @Valid @RequestBody CopilotSetModCopilotsReq req) {
        service.removeCopilotIds(req, helper.getUserId());
        return MaaResult.success();
    }

    @Operation(summary = "更新作业集信息")
    @RequireJwt
    @PostMapping("/update")
    public MaaResult<Void> updateCopilotSet(
            @Parameter(description = "更新作业集信息请求") @Valid @RequestBody CopilotSetUpdateReq req) {
        service.update(req, helper.getUserId());
        return MaaResult.success();
    }

    @Operation(summary = "删除作业集")
    @RequireJwt
    @PostMapping("/delete")
    public MaaResult<Void> deleteCopilotSet(
            @Parameter(description = "删除作业集信息请求") @Valid @RequestBody CommonIdReq<Long> req) {
        service.delete(req.getId(), helper.getUserId());
        return MaaResult.success();
    }


}
