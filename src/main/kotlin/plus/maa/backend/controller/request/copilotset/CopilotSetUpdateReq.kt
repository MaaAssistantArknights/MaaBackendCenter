package plus.maa.backend.controller.request.copilotset

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import plus.maa.backend.service.model.CopilotSetStatus

/**
 * @author dragove
 * create on 2024-01-02
 */
@Schema(title = "作业集更新请求")
data class CopilotSetUpdateReq(
    @field:NotNull(message = "作业集id不能为空")
    val id: Long,
    @Schema(title = "作业集名称")
    @field:NotBlank(message = "作业集名称不能为空")
    val name: String,
    @Schema(title = "作业集额外描述")
    val description: String = "",
    @Schema(title = "作业集公开状态", enumAsRef = true)
    @field:NotNull(message = "作业集公开状态不能为null")
    val status: CopilotSetStatus,
)
