package plus.maa.backend.repository.entity.arklevel.parse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.arklevel.LevelParse;
import plus.maa.backend.repository.entity.gamedata.ArkStage;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.repository.entity.gamedata.ArkTower;

@Slf4j
@Component
public class LegionLevelParse extends LevelParse {
    /**
     * Legion level will be tagged like this:<br>
     * LEGION -> POSITION -> StageCode == obt/legion/TOWER_ID/LEVEL_ID<br>
     * eg:<br>
     * 保全派驻 -> 阿卡胡拉丛林 -> LT-1 == obt/legion/lt06/level_lt06_01<br>
     */
    @Override
    public void parse(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("保全派驻");

        ArkStage stage = dataService.findStage(level.getLevelId(), tilePos.getCode(), tilePos.getStageId());
        if (stage == null) {
            log.error("[PARSER]保全派驻关卡未找到stage信息:{}", level.getLevelId());
            return;
        }
        ArkTower tower = dataService.findTower(stage.getZoneId());
        if (tower == null) {
            log.error("[PARSER]保全派驻关卡未找到tower信息:{}, level:{}", stage.getZoneId(), level.getLevelId());
            return;
        }
        level.setCatTwo(tower.getName());
        level.setCatThree(tilePos.getCode());
    }
}
