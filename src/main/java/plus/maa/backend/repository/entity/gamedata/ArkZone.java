package plus.maa.backend.repository.entity.gamedata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArkZone {
    /**
     * 例: main_1
     */
    private String zoneId;
    /**
     * 例: 第一章
     */
    private String zoneNameFirst;
    /**
     * 例: 黑暗时代·下
     */
    private String zoneNameSecond;
}
