package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.common.annotation.JsonSchema;
import plus.maa.backend.config.SpringDocConfig;
import plus.maa.backend.config.security.AuthenticationHelper;
import plus.maa.backend.controller.request.CopilotCUDRequest;
import plus.maa.backend.controller.request.CopilotQueriesRequest;
import plus.maa.backend.controller.request.CopilotRatingReq;
import plus.maa.backend.controller.response.CopilotInfo;
import plus.maa.backend.controller.response.CopilotPageInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.CopilotService;

/**
 * @author LoMu
 * Date  2022-12-25 17:08
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("/copilot")
@Tag(name = "CopilotController", description = "作业本体管理接口")
public class CopilotController {
    private final CopilotService copilotService;

    @Operation(summary = "上传作业")
    @ApiResponse(description = "上传作业结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @JsonSchema
    @PostMapping("/upload")
    public MaaResult<Long> uploadCopilot(
            @Parameter(hidden = true) AuthenticationHelper helper,
            @Parameter(description = "作业操作请求") @RequestBody CopilotCUDRequest request
    ) {
        return MaaResult.success(copilotService.upload(helper.requireUserId(), request.getContent()));
    }

    @Operation(summary = "删除作业")
    @ApiResponse(description = "删除作业结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/delete")
    public MaaResult<Void> deleteCopilot(
            @Parameter(hidden = true) AuthenticationHelper helper,
            @Parameter(description = "作业操作请求") @RequestBody CopilotCUDRequest request
    ) {
        copilotService.delete(helper.requireUserId(), request);
        return MaaResult.success();
    }

    @Operation(summary = "获取作业")
    @ApiResponse(description = "作业信息")
    @GetMapping("/get/{id}")
    public MaaResult<CopilotInfo> getCopilotById(
            @Parameter(hidden = true) AuthenticationHelper helper,
            @Parameter(description = "作业id") @PathVariable("id") Long id
    ) {
        var userIdOrIpAddress = helper.getUserIdOrIpAddress();
        return copilotService.getCopilotById(userIdOrIpAddress, id).map(MaaResult::success)
                .orElse(MaaResult.fail(404, "数据不存在"));
    }


    @Operation(summary = "分页查询作业，提供登录凭据时查询用户自己的作业")
    @ApiResponse(description = "作业信息")
    @GetMapping("/query")
    public MaaResult<CopilotPageInfo> queriesCopilot(
            @Parameter(hidden = true) AuthenticationHelper helper,
            @Parameter(description = "作业查询请求") CopilotQueriesRequest copilotQueriesRequest
    ) {
        return MaaResult.success(copilotService.queriesCopilot(helper.getUserId(), copilotQueriesRequest));
    }

    @Operation(summary = "更新作业")
    @ApiResponse(description = "更新结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @JsonSchema
    @PostMapping("/update")
    public MaaResult<Void> updateCopilot(
            @Parameter(hidden = true) AuthenticationHelper helper,
            @Parameter(description = "作业操作请求") @RequestBody CopilotCUDRequest copilotCUDRequest
    ) {
        copilotService.update(helper.requireUserId(), copilotCUDRequest);
        return MaaResult.success();
    }

    @Operation(summary = "为作业评分")
    @ApiResponse(description = "评分结果")
    @JsonSchema
    @PostMapping("/rating")
    public MaaResult<String> ratesCopilotOperation(
            @Parameter(hidden = true) AuthenticationHelper helper,
            @Parameter(description = "作业评分请求") @RequestBody CopilotRatingReq copilotRatingReq
    ) {
        copilotService.rates(helper.getUserIdOrIpAddress(), copilotRatingReq);
        return MaaResult.success("评分成功");
    }

}
