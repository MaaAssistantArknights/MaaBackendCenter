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
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCopilotFromDto(CopilotDTO copilotDTO, @MappingTarget Copilot copilot);

    Copilot toCopilot(CopilotDTO copilotDto);

    @Mapping(source = "doc.title", target = "title")
    @Mapping(source = "doc.details", target = "detail")
    CopilotInfo toCopilotInfo(Copilot copilot);


}
