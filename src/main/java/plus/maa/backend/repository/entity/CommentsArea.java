package plus.maa.backend.repository.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author LoMu
 * Date  2023-02-17 14:50
 */
@Data
@Accessors(chain = true)
@Document("maa_comments_area")
public class CommentsArea implements Serializable {

    @Id
    private String id;

    @Indexed
    private Long copilotId;

    //答复某个评论
    private String fromCommentId;


    private String uploaderId;

    //评论内容
    private String message;

    private long likeCount;

    private long dislikeCount;

    private LocalDateTime uploadTime = LocalDateTime.now();

    // 是否将该评论置顶
    private boolean topping;

    private boolean delete;

    private LocalDateTime deleteTime;

    //其主评论id(如果自身为主评论则为null)
    private String mainCommentId;

    //邮件通知
    private Boolean notification;

}


