package plus.maa.backend.service.model.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkStage;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.repository.entity.gamedata.ArkTower;
import plus.maa.backend.service.ArkGameDataService;
import plus.maa.backend.service.model.ArkLevelType;

/**
 * @author john180
 * <p>
 * Legion level will be tagged like this:<br>
 * LEGION -> POSITION -> StageCode == obt/legion/TOWER_ID/LEVEL_ID<br>
 * eg:<br>
 * 保全派驻 -> 阿卡胡拉丛林 -> LT-1 == obt/legion/lt06/level_lt06_01<br>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LegionParser implements ArkLevelParser {
    private final ArkGameDataService dataService;

    @Override
    public boolean supportType(ArkLevelType type) {
        return ArkLevelType.LEGION.equals(type);
    }

    @Override
    public ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne(ArkLevelType.LEGION.getDisplay());

        ArkStage stage = dataService.findStage(level.getLevelId(), tilePos.getCode(), tilePos.getStageId());
        if (stage == null) {
            log.error("[PARSER]保全派驻关卡未找到stage信息:{}", level.getLevelId());
            return null;
        }
        ArkTower tower = dataService.findTower(stage.getZoneId());
        if (tower == null) {
            log.error("[PARSER]保全派驻关卡未找到tower信息:{}, level:{}", stage.getZoneId(), level.getLevelId());
            return null;
        }
        level.setCatTwo(tower.getName());
        level.setCatThree(tilePos.getCode());
        return level;
    }
}
