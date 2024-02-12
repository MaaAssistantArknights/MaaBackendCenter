package plus.maa.backend.controller.response.comments

import java.time.LocalDateTime

/**
 * @author LoMu
 * Date  2023-02-20 17:05
 */
data class SubCommentsInfo(
    val commentId: String,
    val uploader: String,
    val uploaderId: String,
    //评论内容,
    val message: String,
    val uploadTime: LocalDateTime,
    val like: Long = 0,
    val dislike: Long = 0,
    val fromCommentId: String,
    val mainCommentId: String,
    val deleted: Boolean = false
)
