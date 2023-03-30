package plus.maa.backend.common.utils.converter;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import plus.maa.backend.controller.request.CopilotDTO;
import plus.maa.backend.controller.response.CopilotInfo;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.MaaUser;

import java.util.Date;


/**
 * @author LoMu
 * Date  2023-01-10 19:10
 */

@Mapper
public interface CopilotConverter {
    CopilotConverter INSTANCE = Mappers.getMapper(CopilotConverter.class);

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
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCopilotFromDto(CopilotDTO copilotDTO, String content, @MappingTarget Copilot copilot);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "deleteTime", ignore = true)
    @Mapping(target = "views", constant = "0L")
    @Mapping(target = "hotScore", constant = "0")
    @Mapping(target = "delete", constant = "false")
    @Mapping(target = "uploadTime", source = "now")
    @Mapping(target = "firstUploadTime", source = "now")
    @Mapping(target = "uploaderId", source = "userId")
    Copilot toCopilot(CopilotDTO copilotDto, String userId, Date now, Long copilotId, String content);

    @Mapping(target = "ratingType", ignore = true)
    @Mapping(target = "ratingRatio", ignore = true)
    @Mapping(target = "ratingLevel", ignore = true)
    @Mapping(target = "notEnoughRating", ignore = true)
    @Mapping(target = "available", ignore = true)
    @Mapping(target = "id", source = "copilotId")
    @Mapping(target = "uploader", source = "userName")
    @Mapping(target = "commentsCount", conditionExpression = "java(commentsCount != null)")
    CopilotInfo toCopilotInfo(Copilot copilot, String userName, Long copilotId, Long commentsCount);
}
