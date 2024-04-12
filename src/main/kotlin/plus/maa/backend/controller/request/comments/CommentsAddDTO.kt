package plus.maa.backend.controller.request.comments

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

/**
 * @author LoMu
 * Date  2023-02-17 14:58
 */
data class CommentsAddDTO(
    /**
     * 评论内容
     */
    @field:Length(min = 1, max = 150, message = "评论内容不可超过150字，请删减")
    @field:NotBlank(message = "请填写评论内容")
    val message: String,
    /**
     * 评论所在作业的 id
     */
    val copilotId: Long,
    /**
     * 被回复评论的 id
     */
    val fromCommentId: String? = null,
    /**
     * 是否接收通知
     */
    val notification: Boolean = true,
)
