package plus.maa.backend.controller.response.copilotset

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

/**
 * @author dragove
 * create on 2024-01-06
 */
@Schema(title = "作业集响应（列表）")
data class CopilotSetListRes (
    @Schema(title = "作业集id")
    val id: Long,

    @Schema(title = "作业集名称")
    val name: String,

    @Schema(title = "额外描述")
    val description: String,

    @Schema(title = "上传者id")
    val creatorId: String,

    @Schema(title = "上传者昵称")
    val creator: String,

    @Schema(title = "创建时间")
    val createTime: LocalDateTime,

    @Schema(title = "更新时间")
    val updateTime: LocalDateTime
)
