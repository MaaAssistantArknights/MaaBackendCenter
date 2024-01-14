package plus.maa.backend.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import plus.maa.backend.service.model.CopilotSetStatus;

/**
 * @author dragove
 * create on 2024-01-02
 */
@Getter
@Setter
@Schema(title = "作业集更新请求")
public class CopilotSetUpdateReq {
    @NotNull(message = "作业集id不能为空")
    private long id;

    @Schema(title = "作业集名称")
    @NotBlank(message = "作业集名称不能为空")
    private String name;

    @Schema(title = "作业集额外描述")
    private String description;

    @NotNull(message = "作业集公开状态不能为null")
    @Schema(title = "作业集公开状态", enumAsRef = true)
    private CopilotSetStatus status;
}
