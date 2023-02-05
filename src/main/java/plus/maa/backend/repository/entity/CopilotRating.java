package plus.maa.backend.repository.entity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author LoMu
 * Date  2023-01-20 11:20
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document("maa_copilot_rating")
public class CopilotRating {
    @Id
    private String id;

    //作业id
    @Indexed
    private Long copilotId;
    //评分用户
    private List<RatingUser> ratingUsers = new ArrayList<>();

    //评级
    private int ratingLevel;

    //评级比率 十分之一代表半颗星
    private double ratingRatio;

    public CopilotRating(Long copilotId) {
        this.copilotId = copilotId;
    }

    @JsonIgnore
    private boolean delete;
    @JsonIgnore
    private Date deleteTime;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RatingUser {
        @Indexed
        private String userId;
        private String rating;
    }
}
