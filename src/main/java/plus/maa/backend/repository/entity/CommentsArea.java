package plus.maa.backend.repository.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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

    private String fromCommentId;

    private String uploader;

    private String uploaderId;
    private String replyTo;

    //评论内容
    private String message;

    //赞 踩
    private List<CopilotRating.RatingUser> ratingUser = new ArrayList<>();

    private long likeCount;

    private Date uploadTime = new Date();

    private boolean delete;

    private Date deleteTime;

    private String mainCommentId;


}


