package plus.maa.backend.controller.request.copilot;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author LoMu
 * Date  2023-01-20 16:25
 */
@Data
public class CopilotRatingReq {
    @NotBlank(message = "评分作业id不能为空")
    private Long id;
    @NotBlank(message = "评分不能为空")
    private String rating;
}
