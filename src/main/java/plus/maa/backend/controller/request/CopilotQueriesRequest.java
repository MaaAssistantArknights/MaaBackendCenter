package plus.maa.backend.controller.request;

import lombok.Data;

/**
 * @author LoMu
 * Date  2022-12-26 2:48
 */

@Data
public class CopilotQueriesRequest {
    private String id;
    private Integer page;
    private Integer limit;
    private String levelKeyword;
    private String operator;
    private String content;
    private String uploader;
    private String document;
    private String uploaderId;
    private Boolean desc;
    private String orderBy;
    private String language;
    private String rating;

}


