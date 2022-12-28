package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.gamedata.ArkActivity;
import plus.maa.backend.repository.entity.gamedata.ArkStage;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.repository.entity.gamedata.ArkZone;

/**
 * @author john180
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArkLevelParserService {
    private final ArkGameDataService dataService;

    /**
     * 具体地图信息生成规则见
     * <a href="https://github.com/MaaAssistantArknights/MaaCopilotServer/blob/main/src/MaaCopilotServer.GameData/GameDataParser.cs">GameDataParser</a>
     * 尚未全部实现 <br>
     * TODO 完成剩余字段实现
     */
    @Nullable
    public ArkLevel parseLevel(ArkTilePos tilePos, String sha) {
        ArkLevel level = ArkLevel.builder()
                .levelId(tilePos.getLevelId())
                .sha(sha)
                .catThree(tilePos.getCode())
                .name(tilePos.getName())
                .width(tilePos.getWidth())
                .height(tilePos.getHeight())
                .build();
        return parseLevel(level, tilePos);
    }

    private ArkLevel parseLevel(ArkLevel level, ArkTilePos tilePos) {
        String[] ids = level.getLevelId().toLowerCase().split("/");
        String type = (ids[0].equals("obt")) ? ids[1] : ids[0];
        return switch (type) {
            case "main", "hard" -> parseMainline(level, tilePos);
            case "weekly", "promote" -> parseWeekly(level, tilePos);
            case "activities" -> parseActivities(level, tilePos);
            case "campaign" -> parseCampaign(level);
            case "memory" -> parseMemory(level);
            case "rune" -> parseRune(level);
            default -> {
                log.error("[PARSER]未知关卡类型:{}", level.getLevelId());
                yield null;
            }
        };
    }

    /**
     * Main story level will be tagged like this:<br>
     * MAINLINE -> CHAPTER_NAME -> StageCode == obt/main/LEVEL_ID<br>
     * eg:<br>
     * 主题曲 -> 序章：黑暗时代·上 -> 0-1 == obt/main/level_main_00-01<br>
     * 主题曲 -> 第四章：急性衰竭 -> S4-7 == obt/main/level_sub_04-3-1<br>
     */
    private ArkLevel parseMainline(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("主题曲");

        String chapterLevelId = level.getLevelId().split("/")[2];                  // level_main_10-02
        String[] chapterStrSplit = chapterLevelId.split("_");                  // level main 10-02
        String diff = parseDifficulty(chapterStrSplit[1]);                   // easy、main
        String stageCodeEncoded = chapterStrSplit[chapterStrSplit.length - 1];                        // 10-02  remark: obt/main/level_easy_sub_09-1-1
        String chapterStr = stageCodeEncoded.split("-")[0];                   // 10 (str)
        int chapter = Integer.parseInt(chapterStr);                           // 10 (int)

        ArkZone zone = dataService.findZone(level.getLevelId(), tilePos.getCode(), tilePos.getStageId());
        if (zone == null) {
            return null;
        }

        String catTwo = parseZoneName(zone);
        level.setCatTwo(catTwo);

        String catThreeEx = (chapter >= 9) ? String.format("（%s）", diff) : "";
        level.setCatThree(level.getCatThree() + catThreeEx);

        return level;
    }

    /**
     * Weekly level will be tagged like this:<br>
     * WEEKLY -> WEEKLY_ZONE_NAME -> StageCode == obt/weekly/LEVEL_ID<br>
     * eg:<br>
     * 资源收集 -> 空中威胁 -> CA-5 == obt/weekly/level_weekly_fly_5<br>
     * 资源收集 -> 身先士卒 -> PR-D-2 == obt/promote/level_promote_d_2<br>
     */
    private ArkLevel parseWeekly(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("资源收集");

        ArkZone zone = dataService.findZone(level.getLevelId(), tilePos.getCode(), tilePos.getStageId());
        if (zone == null) {
            return null;
        }

        level.setCatTwo(zone.getZoneNameSecond());
        return level;
    }

    /**
     * Activity level will be tagged like this:<br>
     * Activity -> ACT_NAME -> StageCode == activities/ACT_ID/LEVEL_ID<br>
     * eg:<br>
     * 活动关卡 -> 战地秘闻 -> SW-EV-1 == activities/act4d0/level_act4d0_01<br>
     */
    private ArkLevel parseActivities(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("活动关卡");

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

    /**
     * Campaign level will be tagged like this:<br>
     * CAMPAIGN -> CAMPAIGN_CODE -> CAMPAIGN_NAME == obt/campaign/LEVEL_ID<br>
     * eg:<br>
     * 剿灭作战	-> 炎国 -> 龙门外环 == obt/campaign/level_camp_02<br>
     */
    private ArkLevel parseCampaign(ArkLevel level) {
        level.setCatOne("剿灭作战");
        return level;
    }

    /**
     * Memory level will be tagged like this:<br>
     * MEMORY -> POSITION -> OPERATOR_NAME == obt/memory/LEVEL_ID<br>
     * eg:<br>
     * 悖论模拟 -> 狙击 -> 克洛丝 == obt/memory/level_memory_kroos_1<br>
     */
    private ArkLevel parseMemory(ArkLevel level) {
        level.setCatOne("悖论模拟");
        return level;
    }

    private ArkLevel parseRune(ArkLevel level) {
        level.setCatOne("危机合约");
        return level;
    }

    private String parseDifficulty(String diff) {
        return switch (diff.toLowerCase()) {
            case "easy" -> "简单";
            case "tough" -> "磨难";
            default -> "标准";
        };
    }

    private String parseZoneName(ArkZone zone) {
        StringBuilder builder = new StringBuilder();
        if (!ObjectUtils.isEmpty(zone.getZoneNameFirst())) {
            builder.append(zone.getZoneNameFirst());
        }
        builder.append(" ");
        if (!ObjectUtils.isEmpty(zone.getZoneNameSecond())) {
            builder.append(zone.getZoneNameSecond());
        }
        return builder.toString().trim();
    }
}
