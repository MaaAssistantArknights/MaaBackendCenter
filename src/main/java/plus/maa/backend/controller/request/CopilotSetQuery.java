package plus.maa.backend.controller.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

/**
 * @author dragove
 * create on 2024-01-06
 */
@Getter
@Setter
@Schema(title = "作业集列表查询接口参数")
public class CopilotSetQuery {

    @Positive(message = "页码必须为大于0的数字")
    @Schema(title = "页码")
    private int page = 1;

    @Schema(title = "单页数据量")
    @PositiveOrZero(message = "单页数据量必须为大于等于0的数字")
    @Max(value = 50, message = "单页大小不得超过50")
    private int limit = 10;

    @Schema(title = "查询关键词")
    private String keyword;

}
