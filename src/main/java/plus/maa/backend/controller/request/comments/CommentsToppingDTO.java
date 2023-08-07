package plus.maa.backend.controller.request.comments;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Lixuhuilll
 * Date  2023-08-17 11:20
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsToppingDTO {
    @NotBlank(message = "评论id不可为空")
    private String commentId;
    // 是否将指定评论置顶
    private boolean topping = true;
}
