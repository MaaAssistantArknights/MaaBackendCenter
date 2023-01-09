package plus.maa.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.common.annotation.CurrentUser;
import plus.maa.backend.controller.request.CopilotRequest;
import plus.maa.backend.controller.request.CopilotUploadRequest;
import plus.maa.backend.controller.response.CopilotPageInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.service.CopilotService;
import plus.maa.backend.service.model.LoginUser;

/**
 * @author LoMu
 * Date  2022-12-25 17:08
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("/copilot")
public class CopilotController {
    private final CopilotService copilotService;

    @PostMapping("/upload")
    public MaaResult<String> uploadCopilot(@CurrentUser LoginUser loginUser,
                                           @RequestBody CopilotUploadRequest request) {
        return copilotService.upload(loginUser, request.getContent());
    }

    @PostMapping("/delete")
    public MaaResult<Void> deleteCopilot(@CurrentUser LoginUser loginUser,
                                         @RequestBody CopilotRequest request) {
        return copilotService.delete(loginUser, request);
    }

    @GetMapping("/get/{id}")
    public MaaResult<Copilot> getCopilotById(@PathVariable("id") String id) {
        return copilotService.getCopilotById(id);
    }


    @GetMapping("/query")
    public MaaResult<CopilotPageInfo> queriesCopilot(CopilotRequest request) {
        return copilotService.queriesCopilot(request);
    }

    @PostMapping("/update")
    public MaaResult<Void> updateCopilot(@CurrentUser LoginUser loginUser,
                                         @RequestBody Copilot copilot) {
        return copilotService.update(loginUser, copilot);
    }

    @PostMapping("/rating")
    public MaaResult<Void> ratesCopilotOperation(CopilotRequest request) {
        return null;
    }

}
