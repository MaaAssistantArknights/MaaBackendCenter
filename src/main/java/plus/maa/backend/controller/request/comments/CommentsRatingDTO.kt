package plus.maa.backend.controller.request.comments;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author LoMu
 * Date  2023-02-19 13:39
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsRatingDTO {
    @NotBlank(message = "评分id不可为空")
    private String commentId;
    @NotBlank(message = "评分不能为空")
    private String rating;
}
