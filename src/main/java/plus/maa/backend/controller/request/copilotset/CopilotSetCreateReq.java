package plus.maa.backend.controller.request.copilotset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import plus.maa.backend.common.model.CopilotSetType;
import plus.maa.backend.service.model.CopilotSetStatus;

import java.util.List;

/**
 * @author dragove
 * create on 2024-01-01
 */
@Getter
@Setter
@Schema(title = "作业集创建请求")
public class CopilotSetCreateReq implements CopilotSetType {

    @Schema(title = "作业集名称")
    @NotBlank(message = "作业集名称不能为空")
    private String name;

    @Schema(title = "作业集额外描述")
    private String description;

    @NotNull(message = "作业id列表字段不能为null")
    @Size(max = 1000, message = "作业集作业列表最大只能为1000")
    @Schema(title = "初始关联作业列表")
    private List<Long> copilotIds;

    @NotNull(message = "作业集公开状态不能为null")
    @Schema(title = "作业集公开状态", enumAsRef = true)
    private CopilotSetStatus status;

}
