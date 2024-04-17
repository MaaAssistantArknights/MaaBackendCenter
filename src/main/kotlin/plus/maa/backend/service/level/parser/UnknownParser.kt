package plus.maa.backend.service.level.parser

import org.springframework.stereotype.Component
import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.gamedata.ArkTilePos
import plus.maa.backend.service.level.ArkLevelType
import java.util.Locale

@Component
class UnknownParser : ArkLevelParser {
    override fun supportType(type: ArkLevelType): Boolean = ArkLevelType.UNKNOWN == type

    override fun parseLevel(level: ArkLevel, tilePos: ArkTilePos): ArkLevel? {
        val ids = level.levelId!!
            .lowercase(Locale.getDefault())
            .split("/".toRegex())
            .dropLastWhile { it.isEmpty() }
            .toTypedArray()
        val type = if ((ids[0] == "obt")) ids[1] else ids[0]

        level.catOne = ArkLevelType.UNKNOWN.display + type
        return level
    }
}
