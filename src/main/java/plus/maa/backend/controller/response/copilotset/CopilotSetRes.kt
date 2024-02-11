package plus.maa.backend.controller.response.copilotset

import io.swagger.v3.oas.annotations.media.Schema
import plus.maa.backend.service.model.CopilotSetStatus
import java.time.LocalDateTime

/**
 * @author dragove
 * create on 2024-01-06
 */
@Schema(title = "作业集响应")
data class CopilotSetRes(
    @Schema(title = "作业集id")
    private val id: Long,

    @Schema(title = "作业集名称")
    private val name: String,

    @Schema(title = "额外描述")
    private val description: String,

    @Schema(title = "作业id列表")
    private val copilotIds: List<Long>,

    @Schema(title = "上传者id")
    private val creatorId: String,

    @Schema(title = "上传者昵称")
    private val creator: String,

    @Schema(title = "创建时间")
    private val createTime: LocalDateTime,

    @Schema(title = "更新时间")
    private val updateTime: LocalDateTime,

    @Schema(title = "作业状态", enumAsRef = true)
    private val status: CopilotSetStatus
)
