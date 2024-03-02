package plus.maa.backend.service.model.parser

import org.springframework.stereotype.Component
import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.gamedata.ArkTilePos
import plus.maa.backend.service.ArkGameDataService
import plus.maa.backend.service.model.ArkLevelType

@Component
class RuneParser(
    private val dataService: ArkGameDataService
) : ArkLevelParser {

    override fun supportType(type: ArkLevelType): Boolean {
        return ArkLevelType.RUNE == type
    }

    override fun parseLevel(level: ArkLevel, tilePos: ArkTilePos): ArkLevel? {
        level.catOne = ArkLevelType.RUNE.display
        level.catTwo = level.stageId
            ?.let { dataService.findCrisisV2InfoById(it) }
            ?.name ?: tilePos.code

        level.catThree = level.name
        return level
    }
}
