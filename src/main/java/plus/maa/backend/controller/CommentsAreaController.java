package plus.maa.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.common.annotation.CurrentUser;
import plus.maa.backend.controller.request.CommentsAddDto;
import plus.maa.backend.controller.request.CommentsDeleteDTO;
import plus.maa.backend.controller.request.CommentsRatingDTO;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.CommentsAreaService;
import plus.maa.backend.service.model.LoginUser;

/**
 * @author LoMu
 * Date  2023-02-17 14:56
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("/comments")
public class CommentsAreaController {

    private final CommentsAreaService commentsAreaService;

    @PostMapping("/add")
    public MaaResult<String> sendComments(@CurrentUser LoginUser loginUser, @RequestBody CommentsAddDto comments) {
        return commentsAreaService.addComments(loginUser, comments);
    }

    @GetMapping("/query")
    public MaaResult<Void> queryCommentsArea() {
        return null;
    }

    @PostMapping("/delete")
    public MaaResult<String> deleteComments(@CurrentUser LoginUser loginUser, @RequestBody CommentsDeleteDTO comments) {
        return commentsAreaService.deleteComments(loginUser, comments.getCopilotId(), comments.getCommentsId());
    }

    @PostMapping("/rating")
    public MaaResult<String> ratesComments(@CurrentUser LoginUser loginUser, @RequestBody CommentsRatingDTO commentsRatingDTO) {
        return commentsAreaService.rates(loginUser, commentsRatingDTO);
    }
}
