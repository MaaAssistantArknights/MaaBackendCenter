package plus.maa.backend.repository.entity.gamedata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArkStage {
    /**
     * 关卡ID, 需转换为全小写后使用<br>
     * 例: Activities/ACT5D0/level_act5d0_ex08
     */
    private String levelId;
    /**
     * 例: act14d7_zone2
     */
    private String zoneId;
    /**
     * 例:  act5d0_ex08
     */
    private String stageId;
    /**
     * 例: CB-EX8
     */
    private String code;
}
