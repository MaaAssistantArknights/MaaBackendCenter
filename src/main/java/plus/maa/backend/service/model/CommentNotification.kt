package plus.maa.backend.service.model

/**
 * @author LoMu
 * Date  2023-05-18 1:18
 */
data class CommentNotification(
    val authorName: String,
    val reName: String,
    val date: String,
    val title: String,
    val reMessage: String
)
