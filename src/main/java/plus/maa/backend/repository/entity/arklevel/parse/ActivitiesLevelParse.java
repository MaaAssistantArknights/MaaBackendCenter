package plus.maa.backend.repository.entity.arklevel.parse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.arklevel.LevelParse;
import plus.maa.backend.repository.entity.gamedata.ArkActivity;
import plus.maa.backend.repository.entity.gamedata.ArkStage;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;

@Slf4j
@Component
public class ActivitiesLevelParse extends LevelParse {
    /**
     * Activity level will be tagged like this:<br>
     * Activity -> ACT_NAME -> StageCode == activities/ACT_ID/LEVEL_ID<br>
     * eg:<br>
     * 活动关卡 -> 战地秘闻 -> SW-EV-1 == activities/act4d0/level_act4d0_01<br>
     */
    @Override
    public void parse(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("活动关卡");

        ArkStage stage = dataService.findStage(level.getLevelId(), tilePos.getCode(), tilePos.getStageId());
        if (stage == null) {
            log.error("[PARSER]活动关卡未找到stage信息:{}", level.getLevelId());
            return;
        }

        ArkActivity act = dataService.findActivityByZoneId(stage.getZoneId());
        if (act == null) {
            log.error("[PARSER]活动关卡未找到activity信息:{}", level.getLevelId());
            return;
        }

        level.setCatTwo(act.getName());
    }
}
