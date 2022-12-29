package plus.maa.backend.repository.entity.arklevel.parse;

import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.arklevel.LevelParse;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.repository.entity.gamedata.ArkZone;

@Component
public class WeeklyLevelParse extends LevelParse {
    /**
     * Weekly level will be tagged like this:<br>
     * WEEKLY -> WEEKLY_ZONE_NAME -> StageCode == obt/weekly/LEVEL_ID<br>
     * eg:<br>
     * 资源收集 -> 空中威胁 -> CA-5 == obt/weekly/level_weekly_fly_5<br>
     * 资源收集 -> 身先士卒 -> PR-D-2 == obt/promote/level_promote_d_2<br>
     */
    @Override
    public void parse(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("资源收集");

        ArkZone zone = dataService.findZone(level.getLevelId(), tilePos.getCode(), tilePos.getStageId());
        if (zone == null) {
            return;
        }

        level.setCatTwo(zone.getZoneNameSecond());
    }
}
