package plus.maa.backend.service.level.parser

import org.springframework.stereotype.Component
import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.gamedata.ArkTilePos
import plus.maa.backend.service.level.ArkLevelType

/**
 * @author john180
 *
 *
 * Campaign level will be tagged like this:<br></br>
 * CAMPAIGN -> CAMPAIGN_CODE -> CAMPAIGN_NAME == obt/campaign/LEVEL_ID<br></br>
 * eg:<br></br>
 * 剿灭作战	-> 炎国 -> 龙门外环 == obt/campaign/level_camp_02<br></br>
 */
@Component
class CampaignParser : ArkLevelParser {
    override fun supportType(type: ArkLevelType): Boolean {
        return ArkLevelType.CAMPAIGN == type
    }

    override fun parseLevel(level: ArkLevel, tilePos: ArkTilePos): ArkLevel? {
        level.catOne = ArkLevelType.CAMPAIGN.display
        level.catTwo = tilePos.code
        level.catThree = level.name
        return level
    }
}
