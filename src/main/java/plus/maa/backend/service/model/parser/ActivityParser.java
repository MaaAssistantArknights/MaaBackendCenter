package plus.maa.backend.service.model.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkActivity;
import plus.maa.backend.repository.entity.gamedata.ArkStage;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.service.ArkGameDataService;
import plus.maa.backend.service.model.ArkLevelType;

/**
 * @author john180
 * <p>
 * Activity level will be tagged like this:<br>
 * Activity -> ACT_NAME -> StageCode == activities/ACT_ID/LEVEL_ID<br>
 * eg:<br>
 * 活动关卡 -> 战地秘闻 -> SW-EV-1 == activities/act4d0/level_act4d0_01<br>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityParser implements ArkLevelParser {
    private final ArkGameDataService dataService;

    @Override
    public boolean supportType(ArkLevelType type) {
        return ArkLevelType.ACTIVITIES.equals(type);
    }

    @Override
    public ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne(ArkLevelType.ACTIVITIES.getDisplay());

        ArkStage stage = dataService.findStage(level.getLevelId(), tilePos.getCode(), tilePos.getStageId());
        if (stage == null) {
            log.error("[PARSER]活动关卡未找到stage信息:{}", level.getLevelId());
            return null;
        }

        ArkActivity act = dataService.findActivityByZoneId(stage.getZoneId());
        if (act == null) {
            log.error("[PARSER]活动关卡未找到activity信息:{}", level.getLevelId());
            return null;
        }

        level.setCatTwo(act.getName());
        return level;
    }
}
