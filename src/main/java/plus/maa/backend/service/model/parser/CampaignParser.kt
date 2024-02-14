package plus.maa.backend.service.model.parser;

import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.service.model.ArkLevelType;

/**
 * @author john180
 * <p>
 * Campaign level will be tagged like this:<br>
 * CAMPAIGN -> CAMPAIGN_CODE -> CAMPAIGN_NAME == obt/campaign/LEVEL_ID<br>
 * eg:<br>
 * 剿灭作战	-> 炎国 -> 龙门外环 == obt/campaign/level_camp_02<br>
 */
@Component
public class CampaignParser implements ArkLevelParser {
    @Override
    public boolean supportType(ArkLevelType type) {
        return ArkLevelType.CAMPAIGN.equals(type);
    }

    @Override
    public ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne(ArkLevelType.CAMPAIGN.getDisplay());
        level.setCatTwo(tilePos.getCode());
        level.setCatThree(level.getName());
        return level;
    }
}
