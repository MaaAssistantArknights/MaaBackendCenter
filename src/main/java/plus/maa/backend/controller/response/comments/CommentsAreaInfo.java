package plus.maa.backend.controller.response.comments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;


/**
 * @author LoMu
 * Date  2023-02-19 11:47
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class CommentsAreaInfo {
    private Boolean hasNext;
    private Integer page;
    private Long total;
    private List<CommentsInfo> data;
}
