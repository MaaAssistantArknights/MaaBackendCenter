package plus.maa.backend.controller.request.copilot

import jakarta.validation.constraints.NotBlank

/**
 * @author LoMu
 * Date  2023-01-20 16:25
 */
data class CopilotRatingReq(
    @NotBlank(message = "评分作业id不能为空")
    val id: Long,
    @NotBlank(message = "评分不能为空")
    val rating: String,
)
