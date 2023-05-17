package plus.maa.backend.service.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author LoMu
 * Date  2023-05-18 1:18
 */

@Data
@Accessors(chain = true)
public class CommentNotification {
    private String name;
    private String reName;
    private String date;
    private String title;
    private String reMessage;
}
