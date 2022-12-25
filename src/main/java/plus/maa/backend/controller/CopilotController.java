package plus.maa.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import plus.maa.backend.controller.request.CopilotRequest;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.repository.entity.CopiltoPeration;

/**
 * @author LoMu
 * Date  2022-12-25 17:08
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("copilot")
public class CopilotController {

    @PostMapping("upload")
    public MaaResult<CopiltoPeration> uploadCoplilot(@RequestBody CopiltoPeration copiltoPeration) {
        return null;
    }

    @PostMapping("delete")
    public MaaResult<Void> deleteCoplilot(CopilotRequest request) {
        return null;
    }

    @GetMapping("get")
    public MaaResult<Void> getCoplilotById(CopilotRequest request) {
        System.out.println(request.getId().toString() + request.getUploaderId());
        return null;
    }


    @GetMapping("query")
    //id
    public MaaResult<CopiltoPeration> queriesCopilotCopiltot(CopilotRequest request) {
        System.out.println(request.getId());
        return null;
    }

    @PostMapping("update")
    public MaaResult<Void> updatesCopilot(CopiltoPeration copiltoPeration) {
        return null;
    }

    @PostMapping("rating")
    public MaaResult<Void> ratesCopilotOperation() {
        return null;
    }

}
