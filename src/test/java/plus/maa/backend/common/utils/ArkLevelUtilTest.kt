package plus.maa.backend.common.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static plus.maa.backend.common.utils.ArkLevelUtil.getKeyInfoById;

class ArkLevelUtilTest {

    @Test
    void testGetKeyInfoById() {

        var ids = Map.ofEntries(
                entry("level_rune_09-01", "level_rune_09-02"),
                entry("level_crisis_v2_01-07", "crisis_v2_season_1_1"),
                entry("a001_01_perm", "a001_ex05"),
                entry("act11d0_ex08#f#", "act11d0_s02"),
                entry("act11mini_03#f#", "act11mini_04"),
                entry("act17side_01", "act17side_s01_a"),
                entry("act17side_01_rep", "act17side_02_perm")
        );

        var idsWithInfo = Map.ofEntries(
                entry("level_rune_09-01", "rune_9"),
                entry("level_crisis_v2_01-07", "crisis_v2_1"),
                entry("a001_01_perm", "a1"),
                entry("act11d0_ex08#f#", "act11d0"),
                entry("act11mini_03#f#", "act11mini"),
                entry("act17side_01", "act17side"),
                entry("act17side_01_rep", "act17side")
        );

        for (var entity : ids.entrySet()) {
            var key = entity.getKey();
            var value = entity.getValue();
            var infoOfKey = getKeyInfoById(key);
            assertEquals(
                    infoOfKey, getKeyInfoById(value),
                    () -> key + " 与 " + value + " 的地图标识不相同"
            );

            var infoOfMap = idsWithInfo.get(key);
            assertEquals(
                    infoOfKey, infoOfMap,
                    () -> key + " 的地图标识不为 " + infoOfMap
            );
        }
    }
}