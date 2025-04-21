package plus.maa.backend.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import plus.maa.backend.controller.response.user.MaaUserInfo
import java.time.LocalDateTime

@Document("user_following")
data class UserFollowing(
    @Id
    val id: String? = null,
    val userId: String,
    val followList: MutableSet<MaaUserInfo> = mutableSetOf(),
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
