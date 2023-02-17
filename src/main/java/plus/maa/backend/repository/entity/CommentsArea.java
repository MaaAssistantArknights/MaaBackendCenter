package plus.maa.backend.repository.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

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
public class CommentsArea {

    @Id
    private String id;

    @Indexed
    private Long copilotId;

    //评论用户及信息
    private List<CommentsInfo> commentsInfos = new ArrayList<>();
    //子评论
    private List<SubCommentsInfo> subCommentsInfos = new ArrayList<>();


    @Data
    @Accessors(chain = true)
    public static class CommentsInfo {

        private String commentsId;

        private String uploader;
        private String uploaderId;

        //评论内容
        private String message;

        //赞 踩
        private List<CopilotRating.RatingUser> ratingUsers;


        private Date createTime = new Date();

        private Date updateTime = new Date();

        private Boolean delete = false;

        private Date deleteTime;

    }

    @Data
    @Accessors(chain = true)
    public static class SubCommentsInfo {
        private String commentsId;
        private String fromCommentsId;
        private String fromSubCommentsId;
        private String uploader;
        private String uploaderId;

        //评论内容
        private String message;

        //赞 踩
        private List<CopilotRating.RatingUser> ratingUsers;

        private Date createTime = new Date();

        private Date updateTime = new Date();

        private Boolean delete = false;

        private Date deleteTime;

    }

}


