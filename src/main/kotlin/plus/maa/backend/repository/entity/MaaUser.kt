package plus.maa.backend.repository.entity

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable
import java.time.Instant

/**
 * @author AnselYuki
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("maa_user")
data class MaaUser(
    @Id
    val userId: String? = null,
    var userName: String,
    @Indexed(unique = true)
    val email: String,
    var password: String,
    var status: Int = 0,
    val following: MutableSet<String> = mutableSetOf(), // 关注的用户ID集合
    val followers: MutableSet<String> = mutableSetOf(), // 粉丝用户ID集合
    var pwdUpdateTime: Instant = Instant.MIN,
) : Serializable {

    companion object {
        @Transient
        val UNKNOWN: MaaUser = MaaUser(
            userId = "",
            userName = "未知用户:(",
            email = "unknown@unkown.unkown",
            password = "unknown",
        )
    }
}
