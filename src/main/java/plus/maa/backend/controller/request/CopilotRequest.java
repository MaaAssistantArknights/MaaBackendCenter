package plus.maa.backend.controller.request;

import lombok.Data;

/**
 * @author LoMu
 * Date  2022-12-26 2:48
 */

@Data
public class CopilotRequest {
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
    private String orderby;
    private String language;

    private String rating;

    public void setLevel_keyword(String levelKeyword) {
        this.levelKeyword = levelKeyword;
    }

    public void setUploader_id(String uploaderId) {
        this.uploaderId = uploaderId;
    }
}


  /*
    @RequestParam("page") Integer page,
    @RequestParam("limit") Integer limit,
    @RequestParam("level_keyword") String levelKeyword,
    @RequestParam("operator") String operator,
    @RequestParam("content") String content,
    @RequestParam("uploader") String uploader,
    @RequestParam("uploader_id") String uploaderId,
    @RequestParam("desc") String desc,
    @RequestParam("order_by") String orderBy,
    @RequestParam("language") String language*/