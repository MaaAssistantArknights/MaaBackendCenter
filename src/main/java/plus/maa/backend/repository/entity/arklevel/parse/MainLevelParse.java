package plus.maa.backend.repository.entity.arklevel.parse;

import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.arklevel.LevelParse;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.repository.entity.gamedata.ArkZone;

@Component
public class MainLevelParse extends LevelParse {
    /**
     * Main story level will be tagged like this:<br>
     * MAINLINE -> CHAPTER_NAME -> StageCode == obt/main/LEVEL_ID<br>
     * eg:<br>
     * 主题曲 -> 序章：黑暗时代·上 -> 0-1 == obt/main/level_main_00-01<br>
     * 主题曲 -> 第四章：急性衰竭 -> S4-7 == obt/main/level_sub_04-3-1<br>
     */
    @Override
    public void parse(ArkLevel level, ArkTilePos tilePos) {
        level.setCatOne("主题曲");

        String chapterLevelId = level.getLevelId().split("/")[2];                  // level_main_10-02
        String[] chapterStrSplit = chapterLevelId.split("_");                  // level main 10-02
        String diff = parseDifficulty(chapterStrSplit[1]);                   // easy、main
        String stageCodeEncoded = chapterStrSplit[chapterStrSplit.length - 1];                        // 10-02  remark: obt/main/level_easy_sub_09-1-1
        String chapterStr = stageCodeEncoded.split("-")[0];                   // 10 (str)
        int chapter = Integer.parseInt(chapterStr);                           // 10 (int)

        ArkZone zone = dataService.findZone(level.getLevelId(), tilePos.getCode(), tilePos.getStageId());
        if (zone == null) {
            return;
        }

        String catTwo = parseZoneName(zone);
        level.setCatTwo(catTwo);

        String catThreeEx = (chapter >= 9) ? String.format("(%s)", diff) : "";
        level.setCatThree(level.getCatThree() + catThreeEx);
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
