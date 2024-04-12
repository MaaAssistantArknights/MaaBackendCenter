package plus.maa.backend.repository.entity

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.io.Serializable

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
    var refreshJwtIds: MutableList<String> = ArrayList(),
) : Serializable {

    companion object {
        @Transient
        val UNKNOWN: MaaUser = MaaUser(
            userName = "未知用户:(",
            email = "unknown@unkown.unkown",
            password = "unknown",
        )
    }
}
