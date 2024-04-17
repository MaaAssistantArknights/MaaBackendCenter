package plus.maa.backend.service.level.parser

import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.gamedata.ArkTilePos
import plus.maa.backend.service.level.ArkLevelType

/**
 * @author john180
 */
interface ArkLevelParser {
    /**
     * 是否支持解析该关卡类型
     *
     * @param type 关卡类型
     * @return 是否支持
     */
    fun supportType(type: ArkLevelType): Boolean

    /**
     * 解析关卡
     */
    fun parseLevel(level: ArkLevel, tilePos: ArkTilePos): ArkLevel?
}
