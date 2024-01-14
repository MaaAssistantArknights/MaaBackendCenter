package plus.maa.backend.controller.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import plus.maa.backend.service.model.CopilotSetStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author dragove
 * create on 2024-01-06
 */
@Getter
@Setter
@Schema(title = "作业集响应")
public class CopilotSetRes {

    @Schema(title = "作业集id")
    private long id;

    @Schema(title = "作业集名称")
    private String name;

    @Schema(title = "额外描述")
    private String description;

    @Schema(title = "作业id列表")
    private List<Long> copilotIds;

    @Schema(title = "上传者id")
    private String creatorId;

    @Schema(title = "上传者昵称")
    private String creator;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

    @Schema(title = "作业状态", enumAsRef = true)
    private CopilotSetStatus status;


}
