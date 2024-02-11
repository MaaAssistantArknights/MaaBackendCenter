package plus.maa.backend.common.model

import org.springframework.util.Assert

/**
 * @author dragove
 * create on 2024-01-01
 */
interface CopilotSetType {
    val copilotIds: MutableList<Long>

    fun distinctIdsAndCheck(): MutableList<Long> {
        if (copilotIds.isEmpty() || copilotIds.size == 1) {
            return this.copilotIds
        }
        val copilotIds = copilotIds.stream().distinct().toList()
        Assert.state(copilotIds.size <= 1000, "作业集总作业数量不能超过1000条")
        return copilotIds
    }
}
