package plus.maa.backend.controller.request.comments

import jakarta.validation.constraints.NotBlank
import org.hibernate.validator.constraints.Length

/**
 * @author LoMu
 * Date  2023-02-17 14:58
 */
data class CommentsAddDTO(
    // 评论内容
    @field:Length(min = 1, max = 150, message = "评论内容不可超过150字，请删减")
    @field:NotBlank(message = "请填写评论内容")
    val message: String,
    // 评论的作业id
    @field:NotBlank(message = "作业id不可为空")
    val copilotId: String,
    // 子评论来源评论id(回复评论)
    val fromCommentId: String? = null,
    val notification: Boolean = true
)
