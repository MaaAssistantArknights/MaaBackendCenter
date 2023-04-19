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
import plus.maa.backend.controller.request.copilot.CopilotCUDRequest;
import plus.maa.backend.controller.request.copilot.CopilotQueriesRequest;
import plus.maa.backend.controller.request.copilot.CopilotRatingReq;
import plus.maa.backend.controller.response.copilot.CopilotInfo;
import plus.maa.backend.controller.response.copilot.CopilotPageInfo;
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
    private final AuthenticationHelper helper;

    @Operation(summary = "上传作业")
    @ApiResponse(description = "上传作业结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @JsonSchema
    @PostMapping("/upload")
    public MaaResult<Long> uploadCopilot(
            @Parameter(description = "作业操作请求") @RequestBody CopilotCUDRequest request
    ) {
        return MaaResult.success(copilotService.upload(helper.requireUserId(), request.getContent()));
    }

    @Operation(summary = "删除作业")
    @ApiResponse(description = "删除作业结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/delete")
    public MaaResult<Void> deleteCopilot(
            @Parameter(description = "作业操作请求") @RequestBody CopilotCUDRequest request
    ) {
        copilotService.delete(helper.requireUserId(), request);
        return MaaResult.success();
    }

    @Operation(summary = "获取作业")
    @ApiResponse(description = "作业信息")
    @GetMapping("/get/{id}")
    public MaaResult<CopilotInfo> getCopilotById(
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
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            @RequestParam(name = "level_keyword", required = false) String levelKeyword,
            @RequestParam(name = "operator", required = false) String operator,
            @RequestParam(name = "content", required = false) String content,
            @RequestParam(name = "document", required = false) String document,
            @RequestParam(name = "uploader_id", required = false) String uploaderId,
            @RequestParam(name = "desc", required = false, defaultValue = "true") boolean desc,
            @RequestParam(name = "order_by", required = false) String orderBy,
            @RequestParam(name = "language", required = false) String language
    ) {
        var parsed = new CopilotQueriesRequest(
                page,
                limit,
                levelKeyword,
                operator,
                content,
                document,
                uploaderId,
                desc,
                orderBy,
                language
        );
        return MaaResult.success(copilotService.queriesCopilot(helper.getUserId(), parsed));
    }

    @Operation(summary = "更新作业")
    @ApiResponse(description = "更新结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @JsonSchema
    @PostMapping("/update")
    public MaaResult<Void> updateCopilot(
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
            @Parameter(description = "作业评分请求") @RequestBody CopilotRatingReq copilotRatingReq
    ) {
        copilotService.rates(helper.getUserIdOrIpAddress(), copilotRatingReq);
        return MaaResult.success("评分成功");
    }

}
