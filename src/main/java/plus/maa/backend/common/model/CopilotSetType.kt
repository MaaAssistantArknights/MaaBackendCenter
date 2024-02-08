package plus.maa.backend.common.model

import org.springframework.util.Assert

/**
 * @author dragove
 * create on 2024-01-01
 */
interface CopilotSetType {
    val copilotIds: List<Long>?

    val distinctIdsAndCheck: List<Long>?
        get() {
            var copilotIds = copilotIds ?: return emptyList()
            if (copilotIds.isEmpty() || copilotIds.size == 1) {
                return this.copilotIds
            }
            copilotIds = copilotIds.distinct().toList()
            Assert.state(copilotIds.size <= 1000, "作业集总作业数量不能超过1000条")
            return copilotIds
        }
}
