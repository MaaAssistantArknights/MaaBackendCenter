package plus.maa.backend.controller.request.copilot;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author LoMu
 * Date  2022-12-26 2:48
 */
@AllArgsConstructor
@Data
public class CopilotQueriesRequest {
    private int page;
    private int limit;
    private String levelKeyword;
    private String operator;
    private String content;
    private String document;
    private String uploaderId;
    private boolean desc;
    private String orderBy;
    private String language;
}


