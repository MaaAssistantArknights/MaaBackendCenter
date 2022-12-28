package plus.maa.backend.controller.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.experimental.Accessors;
import plus.maa.backend.repository.entity.Copilot;

import java.util.List;

/**
 * @author LoMu
 * Date  2022-12-27 12:39
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
@Data
public class CopilotPageInfo {
    private Boolean hasNext;
    private Integer page;
    private Long total;
    private List<Copilot> data;
}
