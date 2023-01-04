package plus.maa.backend.service.model;

import lombok.Getter;
import org.springframework.util.ObjectUtils;

@Getter
public enum ArkLevelType {
    MAINLINE("主题曲"),
    WEEKLY("资源收集"),
    ACTIVITIES("活动关卡"),
    CAMPAIGN("剿灭作战"),
    MEMORY("悖论模拟"),
    RUNE("危机合约"),
    LEGION("保全派驻"),
    UNKNOWN("未知类型");
    private final String display;

    ArkLevelType(String display) {
        this.display = display;
    }

    public static ArkLevelType fromLevelId(String levelId) {
        if (ObjectUtils.isEmpty(levelId)) {
            return UNKNOWN;
        }
        String[] ids = levelId.toLowerCase().split("/");
        String type = (ids[0].equals("obt")) ? ids[1] : ids[0];
        return switch (type) {
            case "main", "hard" -> MAINLINE;
            case "weekly", "promote" -> WEEKLY;
            case "activities" -> ACTIVITIES;
            case "campaign" -> CAMPAIGN;
            case "memory" -> MEMORY;
            case "rune" -> RUNE;
            case "legion" -> LEGION;
            default -> UNKNOWN;
        };
    }
}
