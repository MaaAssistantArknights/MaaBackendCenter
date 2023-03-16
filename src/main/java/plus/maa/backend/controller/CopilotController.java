package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.common.annotation.CurrentUser;
import plus.maa.backend.common.annotation.JsonSchema;
import plus.maa.backend.controller.request.CopilotCUDRequest;
import plus.maa.backend.controller.request.CopilotQueriesRequest;
import plus.maa.backend.controller.request.CopilotRatingReq;
import plus.maa.backend.controller.response.CopilotInfo;
import plus.maa.backend.controller.response.CopilotPageInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.CopilotService;
import plus.maa.backend.service.model.LoginUser;

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
    @JsonSchema
    @PostMapping("/upload")
    public MaaResult<Long> uploadCopilot(
            @Parameter(description = "登录用户") @CurrentUser LoginUser loginUser,
            @Parameter(description = "作业操作请求") @RequestBody CopilotCUDRequest request) {
        return MaaResult.success(copilotService.upload(loginUser, request.getContent()));
    }

    @Operation(summary = "删除作业")
    @ApiResponse(description = "删除作业结果")
    @PostMapping("/delete")
    public MaaResult<Void> deleteCopilot(@Parameter(description = "登录用户") @CurrentUser LoginUser loginUser,
                                         @Parameter(description = "作业操作请求") @RequestBody CopilotCUDRequest request) {
        copilotService.delete(loginUser, request);
        return MaaResult.success();
    }

    @Operation(summary = "获取作业")
    @ApiResponse(description = "作业信息")
    @GetMapping("/get/{id}")
    public MaaResult<CopilotInfo> getCopilotById(@Parameter(description = "登录用户") @CurrentUser LoginUser user,
                                                 @Parameter(description = "作业id") @PathVariable("id") Long id) {
        return copilotService.getCopilotById(user, id).map(MaaResult::success)
                .orElse(MaaResult.fail(404, "数据不存在"));
    }


    @Operation(summary = "分页查询作业")
    @ApiResponse(description = "作业信息")
    @GetMapping("/query")
    public MaaResult<CopilotPageInfo> queriesCopilot(@Parameter(description = "登录用户") @CurrentUser LoginUser loginUser,
                                                     @Parameter(description = "作业查询请求") CopilotQueriesRequest copilotQueriesRequest) {
        return MaaResult.success(copilotService.queriesCopilot(loginUser, copilotQueriesRequest));
    }

    @Operation(summary = "更新作业")
    @ApiResponse(description = "更新结果")
    @JsonSchema
    @PostMapping("/update")
    public MaaResult<Void> updateCopilot(@Parameter(description = "登录用户") @CurrentUser LoginUser loginUser,
                                         @Parameter(description = "作业操作请求") @RequestBody CopilotCUDRequest copilotCUDRequest) {
        copilotService.update(loginUser, copilotCUDRequest);
        return MaaResult.success();
    }

    @Operation(summary = "为作业评分")
    @ApiResponse(description = "评分结果")
    @JsonSchema
    @PostMapping("/rating")
    public MaaResult<String> ratesCopilotOperation(@Parameter(description = "登录用户") @CurrentUser LoginUser loginUser,
                                                   @Parameter(description = "作业评分请求") @RequestBody CopilotRatingReq copilotRatingReq) {
        copilotService.rates(loginUser, copilotRatingReq);
        return MaaResult.success("评分成功");
    }

}
