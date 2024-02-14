package plus.maa.backend.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import plus.maa.backend.repository.entity.Copilot

/**
 * @author LoMu
 * Date  2022-12-27 10:28
 */
interface CopilotRepository : MongoRepository<Copilot, String> {
    fun findAllByDeleteIsFalse(pageable: Pageable): Page<Copilot>

    fun findFirstByOrderByCopilotIdDesc(): Copilot?

    fun findByCopilotIdAndDeleteIsFalse(copilotId: Long): Copilot?

    fun findByCopilotIdInAndDeleteIsFalse(copilotIds: Collection<Long>): List<Copilot>

    fun findByCopilotId(copilotId: Long): Copilot?

    fun existsCopilotsByCopilotId(copilotId: Long): Boolean
}
