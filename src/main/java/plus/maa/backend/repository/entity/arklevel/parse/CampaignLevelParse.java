package plus.maa.backend.repository.entity.arklevel.parse;

import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.arklevel.LevelParse;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;

@Component
public class CampaignLevelParse extends LevelParse {
    /**
     * Campaign level will be tagged like this:<br>
     * CAMPAIGN -> CAMPAIGN_CODE -> CAMPAIGN_NAME == obt/campaign/LEVEL_ID<br>
     * eg:<br>
     * 剿灭作战	-> 炎国 -> 龙门外环 == obt/campaign/level_camp_02<br>
     */
    @Override
    public void parse(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("剿灭作战");
        level.setCatTwo(tilePos.getCode());
        level.setCatThree(level.getName());
    }
}
