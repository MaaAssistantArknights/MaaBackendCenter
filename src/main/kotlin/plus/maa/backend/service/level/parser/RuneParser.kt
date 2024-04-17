package plus.maa.backend.service.level.parser

import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.gamedata.ArkTilePos
import plus.maa.backend.service.level.ArkGameDataHolder
import plus.maa.backend.service.level.ArkLevelType

class RuneParser(
    private val dataHolder: ArkGameDataHolder,
) : ArkLevelParser {
    override fun supportType(type: ArkLevelType): Boolean {
        return ArkLevelType.RUNE == type
    }

    override fun parseLevel(level: ArkLevel, tilePos: ArkTilePos): ArkLevel? {
        level.catOne = ArkLevelType.RUNE.display
        level.catTwo = level.stageId
            ?.let { dataHolder.findCrisisV2InfoById(it) }
            ?.name ?: tilePos.code

        level.catThree = level.name
        return level
    }
}
