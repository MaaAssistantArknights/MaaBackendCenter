package plus.maa.backend.controller.response.copilot

import java.io.Serializable
import java.time.LocalDateTime

data class CopilotInfo(
    val id: Long,
    val uploadTime: LocalDateTime,
    val uploaderId: String,
    val uploader: String,
    // 用于前端显示的格式化后的干员信息 [干员名]::[技能]
    val views: Long = 0,
    val hotScore: Double = 0.0,
    var available: Boolean = false,
    var ratingLevel: Int = 0,
    var notEnoughRating: Boolean = false,
    var ratingRatio: Double = 0.0,
    var ratingType: Int = 0,
    val commentsCount: Long = 0,
    val content: String,
    val like: Long = 0,
    val dislike: Long = 0,
) : Serializable
