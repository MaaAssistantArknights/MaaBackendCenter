package plus.maa.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author LoMu
 * Date  2023-02-20 17:05
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubCommentsInfo {
    private String commentId;
    private String uploader;
    private String uploaderId;

    //评论内容
    private String message;
    private Date uploadTime;
    private int like;
    private String fromCommentId;
    private String replyTo;
    private String mainCommentId;
}
