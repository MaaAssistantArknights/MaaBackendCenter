package plus.maa.backend.controller.request.comments

import jakarta.validation.constraints.NotBlank
import lombok.AllArgsConstructor
import lombok.Data
import lombok.NoArgsConstructor

/**
 * @author Lixuhuilll
 * Date  2023-08-17 11:20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class CommentsToppingDTO (
    @field:NotBlank(message = "评论id不可为空")
    val commentId:  String,
    // 是否将指定评论置顶
    val topping: Boolean = true
)
