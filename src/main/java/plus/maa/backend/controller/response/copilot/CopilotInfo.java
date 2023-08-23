package plus.maa.backend.controller.response.copilot;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotInfo implements Serializable {
    private Long id;

    private LocalDateTime uploadTime;
    private String uploader;
    //用于前端显示的格式化后的干员信息 [干员名]::[技能]
    private int views;
    private int hotScore;
    private boolean available;
    private int ratingLevel;
    private boolean isNotEnoughRating;
    private double ratingRatio;
    private int ratingType;
    private long commentsCount;
    private String content;
    private long like;
    private long dislike;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean hidden = null;  // 一般情况下不会返回这个字段，只有在查看自己的作业详情时才会返回
}
