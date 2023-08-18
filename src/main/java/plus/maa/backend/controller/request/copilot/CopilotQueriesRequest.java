package plus.maa.backend.controller.request.copilot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Max;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author LoMu
 * Date  2022-12-26 2:48
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotQueriesRequest {
    private int page = 0;
    @Max(value = 50, message = "单页大小不得超过50")
    private int limit = 10;
    private String levelKeyword;
    private String operator;
    private String content;
    private String document;
    private String uploaderId;
    private boolean desc = true;
    private String orderBy;
    private String language;

    /*
     * 这里为了正确接收前端的下划线风格，手动写了三个 setter 用于起别名
     * 因为 Get 请求传入的参数不是 JSON，所以没办法使用 Jackson 的注解直接实现别名
     * 添加 @JsonAlias 和 @JsonIgnore 注解只是为了保障 Swagger 的文档正确显示
     * （吐槽一下，同样是Get请求，怎么CommentsQueries是驼峰命名，到了CopilotQueries就成了下划线命名）
     */
    @JsonIgnore
    public void setLevel_keyword(String levelKeyword) {
        this.levelKeyword = levelKeyword;
    }

    @JsonIgnore
    public void setUploader_id(String uploaderId) {
        this.uploaderId = uploaderId;
    }

    @JsonIgnore
    public void setOrder_by(String orderBy) {
        this.orderBy = orderBy;
    }
}
