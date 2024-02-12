package plus.maa.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.web.bind.annotation.*
import plus.maa.backend.common.annotation.JsonSchema
import plus.maa.backend.common.annotation.SensitiveWordDetection
import plus.maa.backend.config.doc.RequireJwt
import plus.maa.backend.config.security.AuthenticationHelper
import plus.maa.backend.controller.request.comments.*
import plus.maa.backend.controller.response.MaaResult
import plus.maa.backend.controller.response.MaaResult.Companion.success
import plus.maa.backend.controller.response.comments.CommentsAreaInfo
import plus.maa.backend.service.CommentsAreaService

/**
 * @author LoMu
 * Date  2023-02-17 14:56
 */
@RestController
@Tag(name = "CommentArea", description = "评论区管理接口")
@RequestMapping("/comments")
class CommentsAreaController(
    private val commentsAreaService: CommentsAreaService,
    private val authHelper: AuthenticationHelper
) {
    @SensitiveWordDetection("#comments.message")
    @PostMapping("/add")
    @Operation(summary = "发送评论")
    @ApiResponse(description = "发送评论结果")
    @RequireJwt
    fun sendComments(
        @Parameter(description = "评论") @RequestBody comments: @Valid CommentsAddDTO
    ): MaaResult<String> {
        commentsAreaService.addComments(authHelper.requireUserId(), comments)
        return success("评论成功")
    }

    @GetMapping("/query")
    @Operation(summary = "分页查询评论")
    @ApiResponse(description = "评论区信息")
    fun queriesCommentsArea(
        @Parameter(description = "评论查询对象") parsed: @Valid CommentsQueriesDTO
    ): MaaResult<CommentsAreaInfo> {
        return success(commentsAreaService.queriesCommentsArea(parsed))
    }

    @PostMapping("/delete")
    @Operation(summary = "删除评论")
    @ApiResponse(description = "评论删除结果")
    @RequireJwt
    fun deleteComments(
        @Parameter(description = "评论删除对象") @RequestBody comments: @Valid CommentsDeleteDTO
    ): MaaResult<String> {
        commentsAreaService.deleteComments(authHelper.requireUserId(), comments.commentId)
        return success("评论已删除")
    }

    @JsonSchema
    @Operation(summary = "为评论点赞")
    @ApiResponse(description = "点赞结果")
    @RequireJwt
    @PostMapping("/rating")
    fun ratesComments(
        @Parameter(description = "评论点赞对象") @RequestBody commentsRatingDTO: @Valid CommentsRatingDTO
    ): MaaResult<String> {
        commentsAreaService.rates(authHelper.requireUserId(), commentsRatingDTO)
        return success("成功")
    }

    @Operation(summary = "为评论置顶/取消置顶")
    @ApiResponse(description = "置顶/取消置顶结果")
    @RequireJwt
    @PostMapping("/topping")
    fun toppingComments(
        @Parameter(description = "评论置顶对象") @RequestBody commentsToppingDTO: @Valid CommentsToppingDTO
    ): MaaResult<String> {
        commentsAreaService.topping(authHelper.requireUserId(), commentsToppingDTO)
        return success("成功")
    }

    @Operation(summary = "设置通知接收状态")
    @RequireJwt
    @GetMapping("/status")
    fun modifyStatus(@RequestParam id: @NotBlank String, @RequestParam status: Boolean): MaaResult<String> {
        commentsAreaService.notificationStatus(authHelper.userId!!, id, status)
        return success("success")
    }
}
