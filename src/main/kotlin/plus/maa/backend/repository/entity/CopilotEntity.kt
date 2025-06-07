package plus.maa.backend.repository.entity

import com.kotlinorm.annotations.Cascade
import com.kotlinorm.annotations.CreateTime
import com.kotlinorm.annotations.LogicDelete
import com.kotlinorm.annotations.Necessary
import com.kotlinorm.annotations.PrimaryKey
import com.kotlinorm.annotations.Table
import com.kotlinorm.annotations.TableIndex
import com.kotlinorm.annotations.UpdateTime
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
    // @PrimaryKey(identity = true)
    var copilotId: Long? = null,
    // 关卡名
    @Necessary
    var stageName: String? = null,
    // 上传者id
    @Necessary
    var uploaderId: String? = null,
    // 查看次数
    @Necessary
    var views: Long = 0L,
    // 评级
    @Necessary
    var ratingLevel: Int = 0,
    // 评级比率 十分之一代表半星
    @Necessary
    var ratingRatio: Double = 0.0,
    @Necessary
    var likeCount: Long = 0,
    @Necessary
    var dislikeCount: Long = 0,

    // 热度
    @Necessary
    var hotScore: Double = 0.0,

    // 指定干员
    @Cascade(["copilot_id"], ["copilot_id"])
    var opers: List<OperatorEntity>?,

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
    var status: CopilotSetStatus? = CopilotSetStatus.PUBLIC,
    /**
     * 评论状态
     */
    @Necessary
    var commentStatus: CommentStatus? = CommentStatus.ENABLED,

    @LogicDelete
    var delete: Boolean? = false,
    var deleteTime: LocalDateTime? = null,
    var notification: Boolean? = null

) : Serializable, KPojo

@Table("copilot_operator")
@TableIndex("idx_operator_copilot_id", ["copilot_id"])
data class OperatorEntity(
    @PrimaryKey(identity = true)
    var id: Long? = null,
    @Necessary
    var copilotId: Long? = null,
    @Necessary
    var name: String? = null,
) : Serializable, KPojo
