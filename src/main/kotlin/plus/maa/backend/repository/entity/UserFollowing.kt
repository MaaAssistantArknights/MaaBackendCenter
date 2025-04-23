package plus.maa.backend.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("user_following")
data class UserFollowing(
    @Id
    val id: String? = null,
    val userId: String,
    val followList: MutableList<String> = mutableListOf(),
    var updatedAt: Instant = Instant.now(),
)
