package plus.maa.backend.repository.entity

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import java.io.Serializable
import java.time.Instant

@Table("user")
data class UserEntity(
    // 迁移时不标记为主键防止生成
    @PrimaryKey
    var userId: String? = null,
    var userName: String? = null,
    var email: String? = null,
    var password: String? = null,
    var status: Int? = null,
    @ColumnType(type = KColumnType.TIMESTAMP)
    var pwdUpdateTime: Instant? = null,
    var followingCount: Int? = null,
    var fansCount: Int? = null,
) : Serializable, KPojo {
    companion object {
        val UNKNOWN: UserEntity = UserEntity(
            userId = "",
            userName = "未知用户:(",
            email = "unknown@unkown.unkown",
            password = "unknown",
        )
    }
}

