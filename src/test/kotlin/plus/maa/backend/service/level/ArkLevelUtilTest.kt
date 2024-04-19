package plus.maa.backend.service.level

import org.junit.jupiter.api.Test

class ArkLevelUtilTest {
    @Test
    fun testGetKeyInfoById() {
        val ids = mapOf(
            "level_rune_09-01" to "level_rune_09-02",
            "level_crisis_v2_01-07" to "crisis_v2_season_1_1",
            "a001_01_perm" to "a001_ex05",
            "act11d0_ex08#f#" to "act11d0_s02",
            "act11mini_03#f#" to "act11mini_04",
            "act17side_01" to "act17side_s01_a",
            "act17side_01_rep" to "act17side_02_perm",
        )

        val idsWithInfo = mapOf(
            "level_rune_09-01" to "rune_9",
            "level_crisis_v2_01-07" to "crisis_v2_1",
            "a001_01_perm" to "a1",
            "act11d0_ex08#f#" to "act11d0",
            "act11mini_03#f#" to "act11mini",
            "act17side_01" to "act17side",
            "act17side_01_rep" to "act17side",
        )

        for ((key, value) in ids) {
            val infoOfKey = ArkLevelUtil.getKeyInfoById(key)
            check(infoOfKey == ArkLevelUtil.getKeyInfoById(value)) {
                "$key 与 $value 的地图标识不相同"
            }

            val infoOfMap = idsWithInfo[key]
            check(infoOfKey == infoOfMap) {
                "$key 的地图标识不为 $infoOfMap"
            }
        }
    }
}
