package plus.maa.backend.service.model.parser;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkCharacter;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.service.ArkGameDataService;
import plus.maa.backend.service.model.ArkLevelType;

/**
 * @author john180
 * <p>
 * Memory level will be tagged like this:<br>
 * MEMORY -> POSITION -> OPERATOR_NAME == obt/memory/LEVEL_ID<br>
 * eg:<br>
 * 悖论模拟 -> 狙击 -> 克洛丝 == obt/memory/level_memory_kroos_1<br>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MemoryParser implements ArkLevelParser {
    private final ArkGameDataService dataService;

    @Override
    public boolean supportType(ArkLevelType type) {
        return ArkLevelType.MEMORY.equals(type);
    }

    @Override
    public ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne(ArkLevelType.MEMORY.getDisplay());

        String[] chIdSplit = level.getStageId().split("_");     //mem_aurora_1
        if (chIdSplit.length != 3) {
            log.error("[PARSER]悖论模拟关卡stageId异常:{}, level:{}", level.getStageId(), level.getLevelId());
            return null;
        }
        String chId = chIdSplit[1];     //aurora
        ArkCharacter character = dataService.findCharacter(chId);
        if (character == null) {
            log.error("[PARSER]悖论模拟关卡未找到角色信息:{}, level:{}", level.getStageId(), level.getLevelId());
            return null;
        }

        level.setCatTwo(parseProfession(character.getProfession()));
        level.setCatThree(character.getName());

        return level;
    }

    private String parseProfession(String professionId) {
        return switch (professionId.toLowerCase()) {
            case "medic" -> "医疗";
            case "special" -> "特种";
            case "warrior" -> "近卫";
            case "sniper" -> "狙击";
            case "tank" -> "重装";
            case "caster" -> "术师";
            case "pioneer" -> "先锋";
            case "support" -> "辅助";
            default -> "未知";
        };
    }
}
