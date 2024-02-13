package plus.maa.backend.repository.entity.gamedata;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArkCrisisV2Info {
    private String seasonId;
    private String name;
    // 时间戳，单位：秒
    private long startTs;
    private long endTs;
}
