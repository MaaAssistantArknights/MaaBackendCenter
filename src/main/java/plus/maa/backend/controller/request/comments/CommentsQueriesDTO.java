package plus.maa.backend.controller.request.comments;

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
    private Long copilotId;
    private int page;
    private int limit;
    private boolean desc;
    private String orderBy;
}
