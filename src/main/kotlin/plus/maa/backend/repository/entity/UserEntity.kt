package plus.maa.backend.repository.entity

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.Necessary
import com.kotlinorm.annotations.Table
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import java.io.Serializable
import java.sql.Timestamp

@Table("user")
data class UserEntity(
    // 迁移时不标记为主键防止生成
//    @PrimaryKey(uuid = true)
    val userId: String? = null,
    @Necessary
    var userName: String? = null,
    @Necessary
    val email: String? = null,
    @Necessary
    var password: String? = null,
    @Necessary
    var status: Int? = 0,
    @Necessary
    @ColumnType(KColumnType.TEXT)
    var pwdUpdateTime: Timestamp? = Timestamp(0L),
    @Necessary
    var followingCount: Int? = 0,
    @Necessary
    var fansCount: Int? = 0,
) : Serializable, KPojo

