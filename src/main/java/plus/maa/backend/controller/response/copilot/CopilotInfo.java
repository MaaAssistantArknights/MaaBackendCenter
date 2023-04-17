package plus.maa.backend.controller.response.copilot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotInfo implements Serializable {
    private Long id;

    private Date uploadTime;
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
}
