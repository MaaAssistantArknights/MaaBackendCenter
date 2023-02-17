package plus.maa.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.common.annotation.CurrentUser;
import plus.maa.backend.controller.request.CommentsRequest;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.CommentsSectionService;
import plus.maa.backend.service.model.LoginUser;

/**
 * @author LoMu
 * Date  2023-02-17 14:56
 */

@RestController
@RequiredArgsConstructor
@RequestMapping("comments")
public class CommentsSectioController {

    private final CommentsSectionService commentsSectionService;

    @PostMapping("send")
    public MaaResult<String> sendComments(@CurrentUser LoginUser loginUser, @RequestBody CommentsRequest comments) {
        return commentsSectionService.sendComments(loginUser, comments);
    }

    @GetMapping("query")
    public MaaResult<Void> queryCommentsSectio() {
        return null;
    }

}
