package plus.maa.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.repository.entity.CopiltoPeration;

import java.util.Map;

/**
 * @author LoMu
 * Date  2022-12-25 17:08
 */

@RequiredArgsConstructor
@RestController
@RequestMapping("copilot")
public class CopilotOperationController {

    @PostMapping("upload")
    public MaaResult<Map<String, String>> uploadCoplilotOperation(@RequestBody CopiltoPeration copiltoPeration) {

        return null;
    }

    @PostMapping("delete")
    public MaaResult<Void> deleteCoplilotOperation(@RequestBody String id) {
        return null;
    }

    @GetMapping("get/{id}{language}")
    public MaaResult<Void> getCoplilotOperationById(@PathVariable("id") String id, @PathVariable String language) {
        return null;
    }

    @GetMapping("query{page}{limit}{level_keyword}{operator}{content}{uploader}{uploader_id}{desc}{order_by}{language}")
    public MaaResult<CopiltoPeration> queriesCopilotOperations(
            @PathVariable Integer page,
            @PathVariable Integer limit,
            @PathVariable("level_keyword") String levelKeyword,
            @PathVariable String operator,
            @PathVariable String content,
            @PathVariable String uploader,
            @PathVariable("uploader_id") String uploaderId,
            @PathVariable String desc,
            @PathVariable String order_by,
            @PathVariable String language) {
        return null;
    }

    @PostMapping("update")
    public MaaResult<Void> updatesCopilotOperation(@RequestBody CopiltoPeration copiltoPeration) {
        return null;
    }

    @PostMapping("rating")
    public MaaResult<Void> ratesCopilotOperation() {
        return null;
    }

}
