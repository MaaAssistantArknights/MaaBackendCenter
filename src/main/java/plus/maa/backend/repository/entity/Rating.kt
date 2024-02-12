package plus.maa.backend.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import plus.maa.backend.service.model.RatingType
import java.time.LocalDateTime

/**
 * @author lixuhuilll
 * Date  2023-08-20 11:20
 */
@Document(collection = "maa_rating") // 复合索引
@CompoundIndexes(CompoundIndex(name = "idx_rating", def = "{'type': 1, 'key': 1, 'userId': 1}", unique = true))
data class Rating(
    @Id
    val id: String? = null,

    // 下面三个字段组成复合索引，一个用户对一个对象只能有一种评级
    val type: KeyType, // 评级的类型，如作业(copilot)、评论(comment)
    val key: String, // 被评级对象的唯一标识，如作业id、评论id
    val userId: String, // 评级的用户id

    var rating: RatingType, // 评级，如 "Like"、"Dislike"、"None"
    var rateTime: LocalDateTime // 评级时间
) {
    enum class KeyType {
        COPILOT, COMMENT
    }
}
