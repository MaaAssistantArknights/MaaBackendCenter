package plus.maa.backend.controller.response.comments

import java.time.LocalDateTime

/**
 * @author LoMu
 * Date  2023-02-20 17:05
 */
data class SubCommentsInfo(
    private val commentId: String,
    private val uploader: String,
    private val uploaderId: String,
    //评论内容,
    private val message: String,
    private val uploadTime: LocalDateTime,
    private val like: Long = 0,
    private val dislike: Long = 0,
    private val fromCommentId: String,
    private val mainCommentId: String,
    private val deleted: Boolean = false
)
