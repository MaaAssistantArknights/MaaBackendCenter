package plus.maa.backend.controller.response.copilot;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.List;

/**
 * @author LoMu
 * Date  2022-12-27 12:39
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
@Data
public class CopilotPageInfo implements Serializable {
    private Boolean hasNext;
    private Integer page;
    private Long total;
    private List<CopilotInfo> data;
}
