package plus.maa.backend.repository.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import nonapi.io.github.classgraph.json.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * @author LoMu
 * Date  2023-02-17 14:50
 */
@Data
@Accessors(chain = true)
@Document("maa_comments_sectio")
public class CommentsSection {
    //内部评论表id
    @Id
    private String id;

    @Indexed
    private String copilotId;

    //评论用户及信息
    private List<CommentsUserInfo> commentsUserInfos;


    @Data
    @Accessors(chain = true)
    public static class CommentsUserInfo {
        private String uploader;
        private String uploaderId;

        //评论内容
        private String content;

        //赞
        private int like;
        //踩
        private int dislike;

        private Date createTime = new Date();

        private Date updateTime = new Date();

        private Boolean delete = false;

        private Date deleteTime;

    }

}


