package plus.maa.backend.repository.entity.arklevel.parse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.arklevel.LevelParse;
import plus.maa.backend.repository.entity.gamedata.ArkCharacter;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;

@Slf4j
@Component
public class MemoryLevelParse extends LevelParse {
    /**
     * Memory level will be tagged like this:<br>
     * MEMORY -> POSITION -> OPERATOR_NAME == obt/memory/LEVEL_ID<br>
     * eg:<br>
     * 悖论模拟 -> 狙击 -> 克洛丝 == obt/memory/level_memory_kroos_1<br>
     */
    @Override
    public void parse(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("悖论模拟");

        String[] chIdSplit = level.getStageId().split("_");     //mem_aurora_1
        if (chIdSplit.length != 3) {
            log.error("[PARSER]悖论模拟关卡stageId异常:{}, level:{}", level.getStageId(), level.getLevelId());
            return;
        }
        String chId = chIdSplit[1];     //aurora
        ArkCharacter character = dataService.findCharacter(chId);
        if (character == null) {
            log.error("[PARSER]悖论模拟关卡未找到角色信息:{}, level:{}", level.getStageId(), level.getLevelId());
            return;
        }

        level.setCatTwo(Profession.getEnum(character.getProfession()).getValue());
        level.setCatThree(character.getName());
    }

    private enum Profession {
        Medic("医疗"),
        Special("特种"),
        Warrior("近卫"),
        Sniper("狙击"),
        Tank("重装"),
        Caster("术师"),
        Pioneer("先锋"),
        Support("辅助"),
        Unknown("未知");
        private final String value;

        Profession(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Profession getEnum(String name) {
            for (Profession v : values())
                if (v.name().equalsIgnoreCase(name)) return v;
            return Profession.Unknown;
        }
    }
}
