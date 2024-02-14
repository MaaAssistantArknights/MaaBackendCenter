package plus.maa.backend.service.model.parser

import org.springframework.stereotype.Component
import org.springframework.util.ObjectUtils
import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.gamedata.ArkTilePos
import plus.maa.backend.repository.entity.gamedata.ArkZone
import plus.maa.backend.service.ArkGameDataService
import plus.maa.backend.service.model.ArkLevelType
import java.util.*

/**
 * @author john180
 *
 *
 * Main story level will be tagged like this:<br></br>
 * MAINLINE -> CHAPTER_NAME -> StageCode == obt/main/LEVEL_ID<br></br>
 * eg:<br></br>
 * 主题曲 -> 序章：黑暗时代·上 -> 0-1 == obt/main/level_main_00-01<br></br>
 * 主题曲 -> 第四章：急性衰竭 -> S4-7 == obt/main/level_sub_04-3-1<br></br>
 */
@Component
class MainlineParser(
    private val dataService: ArkGameDataService
) : ArkLevelParser {

    override fun supportType(type: ArkLevelType): Boolean {
        return ArkLevelType.MAINLINE == type
    }

    override fun parseLevel(level: ArkLevel, tilePos: ArkTilePos): ArkLevel? {
        level.catOne = ArkLevelType.MAINLINE.display

        val chapterLevelId =
            level.levelId!!.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2] // level_main_10-02
        val chapterStrSplit =
            chapterLevelId.split("_".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray() // level main 10-02
        val diff = parseDifficulty(chapterStrSplit[1]) // easy、main
        val stageCodeEncoded =
            chapterStrSplit[chapterStrSplit.size - 1] // 10-02  remark: obt/main/level_easy_sub_09-1-1
        val chapterStr =
            stageCodeEncoded.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] // 10 (str)
        val chapter = chapterStr.toInt() // 10 (int)

        val zone = dataService.findZone(level.levelId, tilePos.code!!, tilePos.stageId!!) ?: return null

        val catTwo = parseZoneName(zone)
        level.catTwo = catTwo

        val catThreeEx = if ((chapter >= 9)) String.format("（%s）", diff) else ""
        level.catThree += catThreeEx

        return level
    }

    private fun parseDifficulty(diff: String): String {
        return when (diff.lowercase(Locale.getDefault())) {
            "easy" -> "简单"
            "tough" -> "磨难"
            else -> "标准"
        }
    }

    private fun parseZoneName(zone: ArkZone): String {
        val builder = StringBuilder()
        if (!ObjectUtils.isEmpty(zone.zoneNameFirst)) {
            builder.append(zone.zoneNameFirst)
        }
        builder.append(" ")
        if (!ObjectUtils.isEmpty(zone.zoneNameSecond)) {
            builder.append(zone.zoneNameSecond)
        }
        return builder.toString().trim { it <= ' ' }
    }
}
