package plus.maa.backend.controller.response.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * @author dragove
 * create on 2024-01-06
 */
@Getter
@Setter
@Schema(title = "作业集响应（列表）")
public class CopilotSetListRes {

    @Schema(title = "作业集id")
    private long id;

    @Schema(title = "作业集名称")
    private String name;

    @Schema(title = "额外描述")
    private String description;

    @Schema(title = "上传者id")
    private String creatorId;

    @Schema(title = "上传者昵称")
    private String creator;

    @Schema(title = "创建时间")
    private LocalDateTime createTime;

    @Schema(title = "更新时间")
    private LocalDateTime updateTime;

}
