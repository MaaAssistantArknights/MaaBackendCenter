package plus.maa.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

/**
 * @author LoMu
 * Date  2023-02-17 14:58
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsRequest {
    @Length(min = 1, max = 100)
    private String message;

    @NotBlank(message = "作业id不可为空")
    private String copilotId;

    //子评论(回复评论)
    private String fromCommentsId;

    //子子评论(回复回复评论)
    private String fromSubCommentsId;
}
