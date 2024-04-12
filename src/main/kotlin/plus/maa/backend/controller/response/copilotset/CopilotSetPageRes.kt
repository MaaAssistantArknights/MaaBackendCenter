package plus.maa.backend.controller.response.copilotset

import io.swagger.v3.oas.annotations.media.Schema

/**
 * @author dragove
 * create on 2024-01-06
 */
@Schema(title = "作业集分页返回数据")
data class CopilotSetPageRes(
    @Schema(title = "是否有下一页")
    val hasNext: Boolean = false,
    @Schema(title = "总页数")
    val page: Int = 0,
    @Schema(title = "总数据量")
    val total: Long = 0,
    @Schema(title = "作业集列表")
    val data: MutableList<CopilotSetListRes>,
)
