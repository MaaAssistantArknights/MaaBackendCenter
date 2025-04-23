package plus.maa.backend.repository.entity

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document
import java.time.Instant

@Document("user_fans")
data class UserFans(
    @Id
    val id: String? = null,
    val userId: String,
    val fansList: MutableList<String> = mutableListOf(),
    var updatedAt: Instant = Instant.now(),
)
