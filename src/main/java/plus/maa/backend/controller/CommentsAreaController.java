package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.common.annotation.CurrentUser;
import plus.maa.backend.common.annotation.JsonSchema;
import plus.maa.backend.controller.request.CommentsAddDTO;
import plus.maa.backend.controller.request.CommentsDeleteDTO;
import plus.maa.backend.controller.request.CommentsQueriesDTO;
import plus.maa.backend.controller.request.CommentsRatingDTO;
import plus.maa.backend.controller.response.CommentsAreaInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.CommentsAreaService;
import plus.maa.backend.service.model.LoginUser;

/**
 * @author LoMu
 * Date  2023-02-17 14:56
 */

@RestController
@RequiredArgsConstructor
@Tag(name = "CommentArea")
@RequestMapping("/comments")
public class CommentsAreaController {

    private final CommentsAreaService commentsAreaService;

    @PostMapping("/add")
    public MaaResult<String> sendComments(@CurrentUser LoginUser loginUser, @Valid @RequestBody CommentsAddDTO comments) {
        return commentsAreaService.addComments(loginUser, comments);
    }

    @GetMapping("/query")
    public MaaResult<CommentsAreaInfo> queriesCommentsArea(CommentsQueriesDTO commentsQueriesDTO) {
        return commentsAreaService.queriesCommentsArea(commentsQueriesDTO);
    }

    @PostMapping("/delete")
    public MaaResult<String> deleteComments(@CurrentUser LoginUser loginUser, @Valid @RequestBody CommentsDeleteDTO comments) {
        return commentsAreaService.deleteComments(loginUser, comments.getCommentId());
    }

    @JsonSchema
    @PostMapping("/rating")
    public MaaResult<String> ratesComments(@CurrentUser LoginUser loginUser, @Valid @RequestBody CommentsRatingDTO commentsRatingDTO) {
        return commentsAreaService.rates(loginUser, commentsRatingDTO);
    }
}
