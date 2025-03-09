package plus.maa.backend.common.utils.converter

import org.mapstruct.BeanMapping
import org.mapstruct.Mapper
import org.mapstruct.Mapping
import org.mapstruct.MappingTarget
import org.mapstruct.NullValuePropertyMappingStrategy
import plus.maa.backend.controller.request.copilot.CopilotDTO
import plus.maa.backend.controller.response.copilot.CopilotInfo
import plus.maa.backend.repository.entity.Copilot
import plus.maa.backend.service.model.CopilotSetStatus
import java.time.LocalDateTime

/**
 * @author LoMu
 * Date  2023-01-10 19:10
 */
@Mapper(componentModel = "spring")
interface CopilotConverter {
    /**
     * 实现增量更新
     * 将copilotDto 映射覆盖数据库中的 copilot
     * 映射中跳过空值
     *
     * @param copilotDTO 更新值
     * @param copilot    从数据库中查出的原始值
     */
    @Mapping(target = "deleteTime", ignore = true)
    @Mapping(target = "delete", constant = "false")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "copilotId", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "hotScore", ignore = true)
    @Mapping(target = "uploaderId", ignore = true)
    @Mapping(target = "uploadTime", ignore = true)
    @Mapping(target = "firstUploadTime", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "dislikeCount", ignore = true)
    @Mapping(target = "ratingRatio", ignore = true)
    @Mapping(target = "ratingLevel", ignore = true)
    @Mapping(target = "commentStatus", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    fun updateCopilotFromDto(copilotDTO: CopilotDTO, content: String, @MappingTarget copilot: Copilot, status: CopilotSetStatus)

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleteTime", ignore = true)
    @Mapping(target = "likeCount", ignore = true)
    @Mapping(target = "dislikeCount", ignore = true)
    @Mapping(target = "ratingRatio", ignore = true)
    @Mapping(target = "ratingLevel", ignore = true)
    @Mapping(target = "views", constant = "0L")
    @Mapping(target = "hotScore", constant = "0")
    @Mapping(target = "delete", constant = "false")
    @Mapping(target = "uploadTime", source = "now")
    @Mapping(target = "firstUploadTime", source = "now")
    @Mapping(target = "uploaderId", source = "userId")
    @Mapping(target = "commentStatus", ignore = true)
    fun toCopilot(
        copilotDto: CopilotDTO,
        copilotId: Long,
        userId: String,
        now: LocalDateTime,
        content: String,
        status: CopilotSetStatus,
    ): Copilot

    @Mapping(target = "ratingType", ignore = true)
    @Mapping(target = "ratingRatio", ignore = true)
    @Mapping(target = "ratingLevel", ignore = true)
    @Mapping(target = "notEnoughRating", ignore = true)
    @Mapping(target = "available", ignore = true)
    @Mapping(target = "id", source = "copilotId")
    @Mapping(target = "uploader", source = "userName")
    @Mapping(target = "like", source = "copilot.likeCount")
    @Mapping(target = "dislike", source = "copilot.dislikeCount")
    @Mapping(target = "commentsCount", conditionExpression = "java(commentsCount != null)")
    fun toCopilotInfo(copilot: Copilot, userName: String, copilotId: Long, commentsCount: Long?): CopilotInfo
}
