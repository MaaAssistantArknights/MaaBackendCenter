package plus.maa.backend.controller.request.copilotset

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero

/**
 * @author dragove
 * create on 2024-01-06
 */
@Schema(title = "作业集列表查询接口参数")
data class CopilotSetQuery (
    @Schema(title = "页码")
    val page: @Positive(message = "页码必须为大于0的数字") Int = 1,

    @Schema(title = "单页数据量")
    val limit: @PositiveOrZero(message = "单页数据量必须为大于等于0的数字") @Max(
        value = 50,
        message = "单页大小不得超过50"
    ) Int = 10,

    @Schema(title = "查询关键词")
    val keyword: String? = null
)
