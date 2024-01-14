package plus.maa.backend.controller.request.copilotset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * @author dragove
 * create on 2024-01-02
 */
@Getter
@Setter
@Schema(title = "作业集新增作业列表请求")
public class CopilotSetModCopilotsReq {

    @NotNull(message = "作业集id必填")
    @Schema(title = "作业集id")
    private long id;

    @NotEmpty(message = "添加/删除作业id列表不可为空")
    @Schema(title = "添加/删除收藏的作业id列表")
    private List<Long> copilotIds;

}
