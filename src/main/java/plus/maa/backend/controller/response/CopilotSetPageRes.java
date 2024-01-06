package plus.maa.backend.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import plus.maa.backend.controller.response.user.CopilotSetListRes;

import java.util.List;

/**
 * @author dragove
 * create on 2024-01-06
 */
@Getter
@Setter
@Schema(title = "作业集分页返回数据")
@Accessors(chain = true)
public class CopilotSetPageRes {

    @Schema(title = "是否有下一页")
    private boolean hasNext;
    @Schema(title = "当前页码")
    private int page;
    @Schema(title = "总数据量")
    private long total;
    @Schema(title = "作业集列表")
    private List<CopilotSetListRes> data;

}
