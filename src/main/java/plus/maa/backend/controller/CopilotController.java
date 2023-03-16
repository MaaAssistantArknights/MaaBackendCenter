package plus.maa.backend.controller;

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
@Tag(name = "CopilotController", description = "作业本体管理")
public class CopilotController {
    private final CopilotService copilotService;

    @JsonSchema
    @PostMapping("/upload")
    public MaaResult<Long> uploadCopilot(
            @CurrentUser LoginUser loginUser,
            @RequestBody CopilotCUDRequest request) {
        return MaaResult.success(copilotService.upload(loginUser, request.getContent()));
    }

    @PostMapping("/delete")
    public MaaResult<Void> deleteCopilot(@CurrentUser LoginUser loginUser,
                                         @RequestBody CopilotCUDRequest request) {
        copilotService.delete(loginUser, request);
        return MaaResult.success();
    }

    @GetMapping("/get/{id}")
    public MaaResult<CopilotInfo> getCopilotById(@CurrentUser LoginUser user,
                                                 @PathVariable("id") Long id) {
        return copilotService.getCopilotById(user, id).map(MaaResult::success)
                .orElse(MaaResult.fail(404, "数据不存在"));
    }


    @GetMapping("/query")
    public MaaResult<CopilotPageInfo> queriesCopilot(@CurrentUser LoginUser loginUser,
                                                     CopilotQueriesRequest copilotQueriesRequest) {
        return MaaResult.success(copilotService.queriesCopilot(loginUser, copilotQueriesRequest));
    }

    @JsonSchema
    @PostMapping("/update")
    public MaaResult<Void> updateCopilot(@CurrentUser LoginUser loginUser,
                                         @RequestBody CopilotCUDRequest copilotCUDRequest) {
        copilotService.update(loginUser, copilotCUDRequest);
        return MaaResult.success();
    }

    @JsonSchema
    @PostMapping("/rating")
    public MaaResult<String> ratesCopilotOperation(@CurrentUser LoginUser loginUser,
                                                   @RequestBody CopilotRatingReq copilotRatingReq) {
        copilotService.rates(loginUser, copilotRatingReq);
        return MaaResult.success("评分成功");
    }

}
