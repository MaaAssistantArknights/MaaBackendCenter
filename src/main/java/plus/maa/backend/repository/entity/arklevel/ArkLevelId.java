package plus.maa.backend.repository.entity.arklevel;

import lombok.extern.slf4j.Slf4j;
import plus.maa.backend.repository.entity.arklevel.parse.*;

@Slf4j
public enum ArkLevelId {
    Main(new MainLevelParse()),
    Weekly(new WeeklyLevelParse()),
    Activities(new ActivitiesLevelParse()),
    Campaign(new CampaignLevelParse()),
    Memory(new MemoryLevelParse()),
    Rune(new RuneLevelParse()),
    Legion(new LegionLevelParse()),
    Unknown(new UnknownLevelParse());
    LevelParse parse;

    ArkLevelId(LevelParse parse) {
        this.parse = parse;
    }

    public LevelParse getParse() {
        return parse;
    }

    public static ArkLevelId getEnum(String levelId) {
        String type = LevelParse.parseType(levelId);
        return switch (type) {
            case "main", "hard" -> Main;
            case "weekly", "promote" -> Weekly;
            case "activities" -> Activities;
            case "campaign" -> Campaign;
            case "memory" -> Memory;
            case "rune" -> Rune;
            case "legion" -> Legion;
            default -> {//其它类型地图, 暂时不需要所以不处理
                log.warn("[PARSER]未知关卡类型:{}", levelId);
                yield Unknown;
            }
        };
    }
}
