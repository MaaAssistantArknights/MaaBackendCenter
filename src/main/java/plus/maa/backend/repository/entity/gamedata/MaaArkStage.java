package plus.maa.backend.repository.entity.gamedata;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
// 忽略对服务器无用的数据
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaaArkStage {

    /**
     * 例: CB-EX8
     */
    private String code;

    /**
     * 例:  act5d0_ex08
     */
    private String stageId;
}
