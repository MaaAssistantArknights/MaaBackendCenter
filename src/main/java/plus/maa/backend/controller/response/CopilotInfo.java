package plus.maa.backend.controller.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import plus.maa.backend.repository.entity.Copilot;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotInfo {
    private String id;
    private String minimumRequired;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date uploadTime;
    private String title;
    private String detail;
    private String uploader;
    //用于前端显示的格式化后的干员信息 [干员名]::[技能]
    private List<String> operators;
    private List<String> opers;
    private List<Copilot.Groups> groups;
    private int views;
    private int hotScore;
    private ArkLevelInfo level;
    private boolean available;
    private int ratingLevel;
    private boolean isNotEnoughRating;
    private int ratingType;
    private int difficulty;
    private String content;
}
