package plus.maa.backend.service.level

import java.util.regex.Pattern

object ArkLevelUtil {
    private val NOT_KEY_INFO: Pattern = Pattern.compile(
        // level_、各种难度前缀、season_、前导零、以-或者_划分的后缀
        "^level_|^easy_|^hard_|^tough_|^main_|season_|(?<!\\d)0+(?=\\d)|[-_]+[^-_]+($|[-_]+\\D+$)",
    )

    /**
     * 从 stageId 或者 seasonId 中提取一个地图系列的唯一标识
     *
     * @param id stageId 或者 seasonId
     * @return 地图系列的唯一标识 <br></br>
     * 例如：a1（骑兵与猎人）、act11d0（沃伦姆德的薄暮）、act11mini（未尽篇章）、crisis_v2_1（浊燃作战）
     */
    fun getKeyInfoById(id: String?): String {
        if (id == null) {
            return ""
        }
        // 去除所有非关键信息
        return NOT_KEY_INFO.matcher(id).replaceAll("")
    }
}
