package plus.maa.backend.common.utils;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static plus.maa.backend.common.utils.ArkLevelUtil.getKeyInfoById;

class ArkLevelUtilTest {

    @Test
    void testGetKeyInfoById() {
        Map<String, String> ids = Map.of(
                "level_rune_09-01", "level_rune_09-02",
                "level_crisis_v2_01-07", "crisis_v2_season_1_1",
                "a001_01_perm", "a001_ex05",
                "act11d0_ex08#f#", "act11d0_s02",
                "act11mini_03#f#", "act11mini_04"
        );

        for (var entity : ids.entrySet()) {
            assertEquals(getKeyInfoById(entity.getKey()), getKeyInfoById(entity.getValue()));
        }
    }
}