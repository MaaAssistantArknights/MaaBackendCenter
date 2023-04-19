package plus.maa.backend.controller.response.comments;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author LoMu
 * Date  2023-02-20 17:04
 */

@Data
@NoArgsConstructor
@Accessors(chain = true)
@AllArgsConstructor
public class CommentsInfo {
    private String commentId;
    private String uploader;
    private String uploaderId;

    //评论内容
    private String message;
    private Date uploadTime;
    private int like;
    private List<SubCommentsInfo> subCommentsInfos = new ArrayList<>();
}
