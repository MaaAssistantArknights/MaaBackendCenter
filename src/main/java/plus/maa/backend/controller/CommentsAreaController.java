package plus.maa.backend.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import plus.maa.backend.common.annotation.JsonSchema;
import plus.maa.backend.config.SpringDocConfig;
import plus.maa.backend.config.security.AuthenticationHelper;
import plus.maa.backend.controller.request.comments.CommentsAddDTO;
import plus.maa.backend.controller.request.comments.CommentsDeleteDTO;
import plus.maa.backend.controller.request.comments.CommentsQueriesDTO;
import plus.maa.backend.controller.request.comments.CommentsRatingDTO;
import plus.maa.backend.controller.response.comments.CommentsAreaInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.service.CommentsAreaService;

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
    private final AuthenticationHelper authHelper;

    @PostMapping("/add")
    @Operation(summary = "发送评论")
    @ApiResponse(description = "发送评论结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    public MaaResult<String> sendComments(
            @Parameter(description = "评论") @Valid @RequestBody CommentsAddDTO comments
    ) {
        commentsAreaService.addComments(authHelper.requireUserId(), comments);
        return MaaResult.success("评论成功");
    }

    @GetMapping("/query")
    @Operation(summary = "分页查询评论")
    @ApiResponse(description = "评论区信息")
    public MaaResult<CommentsAreaInfo> queriesCommentsArea(
            @RequestParam(name = "copilot_id") Long copilotId,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit,
            @RequestParam(name = "desc", required = false, defaultValue = "true") boolean desc,
            @RequestParam(name = "order_by", required = false) String orderBy
    ) {
        var parsed = new CommentsQueriesDTO(
                copilotId,
                page,
                limit,
                desc,
                orderBy
        );
        return MaaResult.success(commentsAreaService.queriesCommentsArea(parsed));
    }

    @PostMapping("/delete")
    @Operation(summary = "删除评论")
    @ApiResponse(description = "评论删除结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    public MaaResult<String> deleteComments(
            @Parameter(description = "评论删除对象") @Valid @RequestBody CommentsDeleteDTO comments
    ) {
        commentsAreaService.deleteComments(authHelper.requireUserId(), comments.getCommentId());
        return MaaResult.success("评论已删除");
    }

    @JsonSchema
    @Operation(summary = "为评论点赞")
    @ApiResponse(description = "点赞结果")
    @SecurityRequirement(name = SpringDocConfig.SECURITY_SCHEME_NAME)
    @PostMapping("/rating")
    public MaaResult<String> ratesComments(
            @Parameter(description = "评论点赞对象") @Valid @RequestBody CommentsRatingDTO commentsRatingDTO
    ) {
        commentsAreaService.rates(authHelper.requireUserId(), commentsRatingDTO);
        return MaaResult.success("成功");
    }
}
