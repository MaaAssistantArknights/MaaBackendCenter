package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
@Tag(name = "CommentArea", description = "评论区管理接口")
@RequestMapping("/comments")
public class CommentsAreaController {

    private final CommentsAreaService commentsAreaService;

    @PostMapping("/add")
    @Operation(summary = "发送评论")
    @ApiResponse(description = "发送评论结果")
    public MaaResult<String> sendComments(@Parameter(description = "登录用户") @CurrentUser LoginUser loginUser,
                                          @Parameter(description = "评论") @Valid @RequestBody CommentsAddDTO comments) {
        commentsAreaService.addComments(loginUser, comments);
        return MaaResult.success("评论成功");
    }

    @GetMapping("/query")
    @Operation(summary = "查询评论区")
    @ApiResponse(description = "评论区信息")
    public MaaResult<CommentsAreaInfo> queriesCommentsArea(@Parameter(description = "评论区") CommentsQueriesDTO commentsQueriesDTO) {
        return MaaResult.success(commentsAreaService.queriesCommentsArea(commentsQueriesDTO));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除评论")
    @ApiResponse(description = "评论删除结果")
    public MaaResult<String> deleteComments(@Parameter(description = "登录用户") @CurrentUser LoginUser loginUser,
                                            @Parameter(description = "评论删除对象") @Valid @RequestBody CommentsDeleteDTO comments) {
        commentsAreaService.deleteComments(loginUser, comments.getCommentId());
        return MaaResult.success("评论已删除");
    }

    @JsonSchema
    @PostMapping("/rating")
    @Operation(summary = "为评论点赞")
    @ApiResponse(description = "点赞结果")
    public MaaResult<String> ratesComments(@Parameter(description = "登录用户") @CurrentUser LoginUser loginUser,
                                          @Parameter(description = "评论点赞对象") @Valid @RequestBody CommentsRatingDTO commentsRatingDTO) {
        commentsAreaService.rates(loginUser, commentsRatingDTO);
        return MaaResult.success("成功");
    }
}
