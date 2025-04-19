package plus.maa.backend.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

@Document("maa_user_flow")
@CompoundIndexes(
    CompoundIndex(name = "idx_user_follow", def = "{'userId': 1, 'followUserId': 1, 'status': 1}"),
    CompoundIndex(name = "idx_follow_user", def = "{'followUserId': 1, 'status': 1}")
)
data class UserFlow(
    @Id
    val id: String? = null,

    /**
     * 关注者ID
     */
    val userId: String,

    /**
     * 被关注者ID
     */
    val followUserId: String,

    /**
     * 关注时间
     */
    val createTime: LocalDateTime = LocalDateTime.now(),

    /**
     * 关注状态：1-关注中，0-已取消
     */
    var status: Int = 1,

    /**
     * 取消关注时间
     */
    var updateTime: LocalDateTime? = null
)
