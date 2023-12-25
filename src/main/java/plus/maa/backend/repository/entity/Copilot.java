package plus.maa.backend.repository.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @author LoMu
 * Date 2022-12-25 17:56
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
@Document("maa_copilot")
public class Copilot implements Serializable, SeqGenerated {
    @Id
    // 作业id
    private String id;
    // 自增数字ID
    @Indexed(unique = true)
    private Long copilotId;
    // 关卡名
    @Indexed
    private String stageName;

    // 上传者id
    private String uploaderId;

    // 查看次数
    private Long views = 0L;

    //评级
    private int ratingLevel;

    //评级比率 十分之一代表半星
    private double ratingRatio;

    private long likeCount;

    private long dislikeCount;

    // 热度
    private double hotScore;

    // 难度
    private int difficulty;

    // 版本号(文档中说明:最低要求 maa 版本号，必选。保留字段)

    private String minimumRequired;

    // 指定干员
    private List<Operators> opers;
    // 群组
    private List<Groups> groups;
    // 战斗中的操作
    private List<Action> actions;

    // 描述
    private Doc doc;

    private LocalDateTime firstUploadTime;
    private LocalDateTime uploadTime;

    // 原始数据
    private String content;

    @JsonIgnore
    private boolean delete;
    @JsonIgnore
    private LocalDateTime deleteTime;
    @JsonIgnore
    private Boolean notification;

    @Override
    public Long getGeneratedId() {
        return copilotId;
    }

    @Override
    public String getIdFieldName() {
        return "copilotId";
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class OperationGroup implements Serializable {
        // 干员名
        private String name;
        // 技能序号。可选，默认 1
        private int skill = 1;
        // 技能用法。可选，默认 0
        private int skillUsage;

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Operators implements Serializable {
        // 干员名
        private String name;
        // 技能序号。可选，默认 1
        private int skill = 1;
        // 技能用法。可选，默认 0
        private int skillUsage;
        private Requirements requirements = new Requirements();

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
        public static class Requirements implements Serializable {
            // 精英化等级。可选，默认为 0, 不要求精英化等级
            private int elite;
            // 干员等级。可选，默认为 0
            private int level;

            // 技能等级。可选，默认为 0
            private int skillLevel;
            // 模组编号。可选，默认为 0
            private int module;
            // 潜能要求。可选，默认为 0
            private int potentiality;

        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Groups implements Serializable {
        // 群组名
        private String name;

        private List<OperationGroup> opers;

        private List<String> operators;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Action implements Serializable {
        // 操作类型，可选，默认 "Deploy"
        private String type = "Deploy";
        private int kills;
        private int costs;
        private int costChanges;
        // 默认 -1
        private int cooling = -1;

        private String name;

        // 部署干员的位置。
        private Integer[] location;
        // 部署干员的干员朝向 中英文皆可
        private String direction = "None";
        // 修改技能用法。当 type 为 "技能用法" 时必选
        private int skillUsage;
        // 前置延时
        private int preDelay;
        // 后置延时
        private int postDelay;
        // maa:保留字段，暂未实现
        private long timeout;

        // 描述
        private String doc = "";
        private String docColor = "Gray";

    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static class Doc implements Serializable {

        private String title;
        private String titleColor = "Gray";
        private String details = "";
        private String detailsColor = "Gray";

    }
}
