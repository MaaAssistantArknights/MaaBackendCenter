package plus.maa.backend.controller.response.comments

/**
 * @author LoMu
 * Date  2023-02-19 11:47
 */
data class CommentsAreaInfo(
    val hasNext: Boolean,
    /**
     * Total number of pages
     */
    val page: Int,
    /**
     * Total number of elements
     */
    val total: Long,
    val data: List<CommentsInfo>,
)
