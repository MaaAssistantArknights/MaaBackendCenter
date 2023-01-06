package plus.maa.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import plus.maa.backend.controller.request.CopilotRequest;
import plus.maa.backend.controller.response.CopilotPageInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.service.CopilotService;

/**
 * @author LoMu
 * Date  2022-12-25 17:08
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("copilot")
public class CopilotController {
    private final CopilotService copilotPerationService;

    @PostMapping("upload")
    public MaaResult<String> uploadCoplilot(@RequestBody Copilot copiltoPeration) {
        return copilotPerationService.upload(copiltoPeration);
    }

    @PostMapping("delete")
    public MaaResult<Void> deleteCoplilot(@RequestBody CopilotRequest request) {
        return copilotPerationService.delete(request);
    }

    @GetMapping("get/{id}")
    public MaaResult<Copilot> getCoplilotById(@PathVariable("id") String id) {
        return copilotPerationService.getCopilotById(id);
    }


    @GetMapping("query")
    public MaaResult<CopilotPageInfo> queriesCopilotCopiltot(CopilotRequest request) {
        return copilotPerationService.queriesCopilot(request);
    }

    @PostMapping("update")
    public MaaResult<Void> updateCopilot(@RequestBody Copilot copilot) {
        return copilotPerationService.update(copilot);
    }

    @PostMapping("rating")
    public MaaResult<Void> ratesCopilotOperation(CopilotRequest request) {
        return null;
    }

}
