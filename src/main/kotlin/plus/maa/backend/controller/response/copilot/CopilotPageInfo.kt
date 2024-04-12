package plus.maa.backend.controller.response.copilot

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.io.Serializable

/**
 * @author LoMu
 * Date  2022-12-27 12:39
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
data class CopilotPageInfo(
    val hasNext: Boolean,
    val page: Int,
    val total: Long,
    val data: List<CopilotInfo>,
) : Serializable
