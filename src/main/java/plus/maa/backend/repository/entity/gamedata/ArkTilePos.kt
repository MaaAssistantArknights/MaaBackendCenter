package plus.maa.backend.repository.entity.gamedata

import com.fasterxml.jackson.databind.PropertyNamingStrategies.LowerCamelCaseStrategy
import com.fasterxml.jackson.databind.annotation.JsonNaming

/**
 * 地图格子数据
 *
 * @author dragove
 * created on 2022/12/23
 */
@JsonNaming(LowerCamelCaseStrategy::class)
data class ArkTilePos(
    val code: String? = null,
    val height: Int = 0,
    val width: Int = 0,
    val levelId: String? = null,
    val name: String? = null,
    val stageId: String? = null,
    private val tiles: List<List<Tile>>? = null,
    private val view: List<List<Double>>? = null,
) {
    @JsonNaming(LowerCamelCaseStrategy::class)
    data class Tile(
        private val tileKey: String? = null,
        private val heightType: Int? = null,
        private val buildableType: Int? = null,
        private val isStart: Boolean? = null,
        private val isEnd: Boolean? = null,
    )
}
