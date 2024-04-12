package plus.maa.backend.repository.entity.gamedata

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

// 小驼峰
@JsonNaming(LowerCamelCaseStrategy::class)
// 忽略对服务器无用的数据
@JsonIgnoreProperties(ignoreUnknown = true)
data class MaaArkStage(
    /**
     * 例: CB-EX8
     */
    val code: String,
    /**
     * 例:  act5d0_ex08
     */
    val stageId: String?,
)
