package plus.maa.backend.repository.entity

import lombok.Data
import lombok.experimental.Accessors
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable
import java.time.LocalDateTime

/**
 * @author LoMu
 * Date  2023-02-17 14:50
 */
@Data
@Accessors(chain = true)
@Document("maa_comments_area")
class CommentsArea(
    @Id
    val id: String? = null,

    @Indexed
    val copilotId: Long,

    // 答复某个评论
    val fromCommentId: String? = null,

    val uploaderId: String,

    // 评论内容
    var message: String,

    var likeCount: Long = 0,

    var dislikeCount: Long = 0,

    val uploadTime: LocalDateTime = LocalDateTime.now(),

    // 是否将该评论置顶
    var topping: Boolean = false,

    var delete: Boolean = false,

    var deleteTime: LocalDateTime? = null,

    // 其主评论id(如果自身为主评论则为null)
    val mainCommentId: String? = null,

    // 邮件通知
    var notification: Boolean = false
) : Serializable


