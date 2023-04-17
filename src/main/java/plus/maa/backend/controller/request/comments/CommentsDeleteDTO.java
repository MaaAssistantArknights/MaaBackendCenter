package plus.maa.backend.controller.request.comments;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author LoMu
 * Date  2023-02-19 10:50
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsDeleteDTO {
    @NotBlank(message = "评论id不可为空")
    private String commentId;
}
