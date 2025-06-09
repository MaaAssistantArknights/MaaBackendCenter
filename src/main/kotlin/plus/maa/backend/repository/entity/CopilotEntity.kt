package plus.maa.backend.repository.entity

import com.kotlinorm.annotations.ColumnType
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.Default
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.Necessary
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Serialize
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.annotations.UpdateTime
import com.kotlinorm.enums.KColumnType
import com.kotlinorm.interfaces.KPojo
import plus.maa.backend.service.model.CommentStatus
import plus.maa.backend.service.model.CopilotSetStatus
import java.io.Serializable
import java.time.LocalDateTime

@Table("copilot")
@TableIndex("idx_copilot_stage_name", ["stage_name"])
@TableIndex("idx_copilot_view", ["views"])
@TableIndex("idx_hot_score", ["hot_score"])
data class CopilotEntity(
    // 迁移时不标记为主键防止生成
    // 自增数字ID
    @PrimaryKey
    var copilotId: Long? = null,
    // 关卡名
    @Necessary
    var stageName: String? = null,
    // 上传者id
    @Necessary
    var uploaderId: String? = null,
    // 查看次数
    @Necessary
    var views: Long? = null,
    // 评级
    @Necessary
    var ratingLevel: Int? = null,
    // 评级比率 十分之一代表半星
    @Necessary
    var ratingRatio: Double? = null,
    @Necessary
    var likeCount: Long? = null,
    @Necessary
    var dislikeCount: Long? = null,

    // 热度
    @Necessary
    var hotScore: Double? = null,

    // 指定干员
//    @Cascade(["copilotId"], ["copilotId"])
//    var opers: List<OperatorEntity>? = null,

    // 文档字段，用于搜索，提取到Copilot类型上
    @Necessary
    var title: String? = null,
    var details: String? = null,

    // 首次上传时间
    @CreateTime
    var firstUploadTime: LocalDateTime? = null,
    // 更新时间
    @UpdateTime
    var uploadTime: LocalDateTime? = null,
    // 原始数据
    @Necessary
    var content: String? = null,
    /**
     * 作业状态，后端默认设置为公开以兼容历史逻辑
     * [plus.maa.backend.service.model.CopilotSetStatus]
     */
    @Necessary
    @Serialize
    @ColumnType(KColumnType.TEXT)
    var status: CopilotSetStatus? = null,
    /**
     * 评论状态
     */
    @Necessary
    @Serialize
    @ColumnType(KColumnType.TEXT)
    var commentStatus: CommentStatus? = null,

    @LogicDelete
    @Default("false")
    @ColumnType(KColumnType.BIT)
    var delete: Boolean? = null,
    var deleteTime: LocalDateTime? = null,
    @Default("false")
    @ColumnType(KColumnType.BIT)
    var notification: Boolean? = null

) : Serializable, KPojo

@Table("copilot_operator")
@TableIndex("idx_operator_name", ["name"])
@TableIndex("idx_operator_copilot_id", ["copilot_id"])
data class OperatorEntity(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    @Necessary
    var copilotId: Long? = null,
    @Necessary
    var name: String? = null,
) : Serializable, KPojo
