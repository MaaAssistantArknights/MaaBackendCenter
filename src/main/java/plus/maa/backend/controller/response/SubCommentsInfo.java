package plus.maa.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author LoMu
 * Date  2023-02-20 17:05
 */

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubCommentsInfo extends CommentsInfo {
    private String fromCommentId;
    private String replyTo;
    private String mainCommentId;
}
