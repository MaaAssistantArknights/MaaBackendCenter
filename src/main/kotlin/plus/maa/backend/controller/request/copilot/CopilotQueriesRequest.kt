package plus.maa.backend.controller.request.copilot

import jakarta.validation.constraints.Max
import org.springframework.web.bind.annotation.BindParam
import plus.maa.backend.service.model.CopilotSetStatus

/**
 * @author LoMu
 * Date  2022-12-26 2:48
 */
data class CopilotQueriesRequest(
    val page: Int = 0,
    @Max(value = 50, message = "单页大小不得超过50")
    val limit: Int = 10,
    @BindParam("level_keyword") var levelKeyword: String? = null,
    val operator: String? = null,
    val content: String? = null,
    val document: String? = null,
    @BindParam("uploader_id") var uploaderId: String? = null,
    val desc: Boolean = true,
    @BindParam("order_by") var orderBy: String? = null,
    val language: String? = null,
    @BindParam("copilot_ids") var copilotIds: List<Long>? = null,
    val status: CopilotSetStatus? = null,
)
