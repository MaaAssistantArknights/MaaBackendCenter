package plus.maa.backend.repository.entity;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import plus.maa.backend.service.model.RatingType;

import java.time.LocalDateTime;

/**
 * @author lixuhuilll
 * Date  2023-08-20 11:20
 */

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "maa_rating")
// 复合索引
@CompoundIndexes({
        // 一个用户对一个对象只能有一种评级
        @CompoundIndex(name = "idx_rating", def = "{'type': 1, 'key': 1, 'userId': 1}", unique = true)
})
public class Rating {
    @Id
    private String id;

    // 下面三个字段组成复合索引，一个用户对一个对象只能有一种评级
    @NotNull
    private KeyType type;   // 评级的类型，如作业(copilot)、评论(comment)
    @NotNull
    private String key;     // 被评级对象的唯一标识，如作业id、评论id
    @NotNull
    private String userId;  // 评级的用户id

    private RatingType rating;      // 评级，如 "Like"、"Dislike"、"None"
    private LocalDateTime rateTime; // 评级时间

    public enum KeyType {
        COPILOT, COMMENT
    }
}
