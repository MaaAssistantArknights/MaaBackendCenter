package plus.maa.backend.service.model.parser;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.repository.entity.gamedata.ArkZone;
import plus.maa.backend.service.ArkGameDataService;
import plus.maa.backend.service.model.ArkLevelType;

/**
 * @author john180
 * <p>
 * Weekly level will be tagged like this:<br>
 * WEEKLY -> WEEKLY_ZONE_NAME -> StageCode == obt/weekly/LEVEL_ID<br>
 * eg:<br>
 * 资源收集 -> 空中威胁 -> CA-5 == obt/weekly/level_weekly_fly_5<br>
 * 资源收集 -> 身先士卒 -> PR-D-2 == obt/promote/level_promote_d_2<br>
 */
@Component
@RequiredArgsConstructor
public class WeeklyParser implements ArkLevelParser {
    private final ArkGameDataService dataService;

    @Override
    public boolean supportType(ArkLevelType type) {
        return ArkLevelType.WEEKLY.equals(type);
    }

    @Override
    public ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne(ArkLevelType.WEEKLY.getDisplay());

        ArkZone zone = dataService.findZone(level.getLevelId(), tilePos.getCode(), tilePos.getStageId());
        if (zone == null) {
            return null;
        }

        level.setCatTwo(zone.getZoneNameSecond());
        return level;
    }
}
