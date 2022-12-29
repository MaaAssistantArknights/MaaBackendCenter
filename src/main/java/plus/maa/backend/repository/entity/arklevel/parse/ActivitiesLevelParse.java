package plus.maa.backend.repository.entity.arklevel.parse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.arklevel.LevelParse;
import plus.maa.backend.repository.entity.gamedata.ArkActivity;
import plus.maa.backend.repository.entity.gamedata.ArkStage;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.repository.entity.gamedata.ArkZone;

@Slf4j
@Component
public class ActivitiesLevelParse extends LevelParse {
    /**
     * Activity level will be tagged like this:<br>
     * Activity -> ACT_NAME -> StageCode == activities/ACT_ID/LEVEL_ID<br>
     * eg:<br>
     * 活动关卡 -> 战地秘闻 -> SW-EV-1 == activities/act4d0/level_act4d0_01<br>
     * 活动关卡 -> 照我以火 荒烟林沼 -> FC-1 == activities/act22side/level_act22side_01
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
        ArkZone zone = dataService.findZone(level.getLevelId(), tilePos.getCode(), tilePos.getStageId());
        if (zone == null) {
            return;
        }
        level.setCatTwo(parseZoneName(act, zone));
    }

    private String parseZoneName(ArkActivity act, ArkZone zone) {
        StringBuilder builder = new StringBuilder();
        builder.append(act.getName());
        if (zone == null) return builder.toString().trim();
        if (!ObjectUtils.isEmpty(zone.getZoneNameFirst())) {
            builder.append(" ");
            builder.append(zone.getZoneNameFirst());
        }
        if (!ObjectUtils.isEmpty(zone.getZoneNameSecond())) {
            builder.append(" ");
            builder.append(zone.getZoneNameSecond());
        }
        return builder.toString().trim();
    }
}
