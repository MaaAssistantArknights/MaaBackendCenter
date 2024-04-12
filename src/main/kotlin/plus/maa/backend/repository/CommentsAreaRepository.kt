package plus.maa.backend.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import plus.maa.backend.repository.entity.CommentsArea

/**
 * @author LoMu
 * Date  2023-02-17 15:06
 */
@Repository
interface CommentsAreaRepository : MongoRepository<CommentsArea, String> {
    fun findByMainCommentId(commentsId: String): List<CommentsArea>

    fun findByCopilotIdAndDeleteAndMainCommentIdExists(
        copilotId: Long,
        delete: Boolean,
        exists: Boolean,
        pageable: Pageable,
    ): Page<CommentsArea>

    fun findByCopilotIdAndUploaderIdAndDeleteAndMainCommentIdExists(
        copilotId: Long,
        uploaderId: String,
        delete: Boolean,
        exists: Boolean,
        pageable: Pageable,
    ): Page<CommentsArea>

    fun findByCopilotIdInAndDelete(copilotIds: Collection<Long>, delete: Boolean): List<CommentsArea>

    fun findByMainCommentIdIn(ids: List<String>): List<CommentsArea>

    fun countByCopilotIdAndDelete(copilotId: Long, delete: Boolean): Long
}
