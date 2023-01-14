package plus.maa.backend.common.utils.converter;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import plus.maa.backend.controller.request.CopilotDTO;
import plus.maa.backend.controller.response.CopilotInfo;
import plus.maa.backend.repository.entity.Copilot;


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
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "arkLevel", ignore = true)
    @Mapping(target = "uploader", ignore = true)
    @Mapping(target = "hotScore", ignore = true)
    @Mapping(target = "uploaderId", ignore = true)
    @Mapping(target = "uploadTime", ignore = true)
    @Mapping(target = "ratingType", ignore = true)
    @Mapping(target = "ratingRatio", ignore = true)
    @Mapping(target = "ratingLevel", ignore = true)
    @Mapping(target = "notEnoughRating", ignore = true)
    @Mapping(target = "firstUploadTime", ignore = true)
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCopilotFromDto(CopilotDTO copilotDTO, @MappingTarget Copilot copilot);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "views", ignore = true)
    @Mapping(target = "hotScore", ignore = true)
    @Mapping(target = "uploader", ignore = true)
    @Mapping(target = "arkLevel", ignore = true)
    @Mapping(target = "uploaderId", ignore = true)
    @Mapping(target = "uploadTime", ignore = true)
    @Mapping(target = "ratingType", ignore = true)
    @Mapping(target = "ratingRatio", ignore = true)
    @Mapping(target = "ratingLevel", ignore = true)
    @Mapping(target = "notEnoughRating", ignore = true)
    @Mapping(target = "firstUploadTime", ignore = true)
    Copilot toCopilot(CopilotDTO copilotDto);

    @Mapping(target = "level", ignore = true)
    @Mapping(target = "content", ignore = true)
    @Mapping(target = "available", ignore = true)
    @Mapping(target = "opers", ignore = true)
    @Mapping(target = "operators", ignore = true)
    @Mapping(source = "doc.title", target = "title")
    @Mapping(source = "doc.details", target = "detail")
    CopilotInfo toCopilotInfo(Copilot copilot);


}
