package plus.maa.backend.controller.request.comments

import jakarta.validation.constraints.NotBlank

/**
 * @author LoMu
 * Date  2023-02-19 13:39
 */
data class CommentsRatingDTO(
    @field:NotBlank(message = "评分id不可为空")
    val commentId: String,
    @field:NotBlank(message = "评分不能为空")
    val rating: String
)
