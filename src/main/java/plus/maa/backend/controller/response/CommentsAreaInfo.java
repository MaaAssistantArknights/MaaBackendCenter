package plus.maa.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


/**
 * @author LoMu
 * Date  2023-02-19 11:47
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentsAreaInfo {
    private String id;
    private String copilotId;
    private int like;
    private Date uploadTime;
}
