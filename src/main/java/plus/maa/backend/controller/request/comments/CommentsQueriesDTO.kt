package plus.maa.backend.controller.request.comments;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author LoMu
 * Date  2023-02-20 17:13
 */


@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsQueriesDTO {
    @NotNull(message = "作业id不可为空")
    private Long copilotId;
    private int page = 0;
    @Max(value = 50, message = "单页大小不得超过50")
    private int limit = 10;
    private boolean desc = true;
    private String orderBy;
    private String justSeeId;
}
