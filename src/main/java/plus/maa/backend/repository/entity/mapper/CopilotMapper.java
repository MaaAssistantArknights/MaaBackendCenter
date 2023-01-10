package plus.maa.backend.repository.entity.mapper;


import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import plus.maa.backend.controller.request.CopilotDTO;
import plus.maa.backend.controller.response.CopilotInfo;
import plus.maa.backend.repository.entity.Copilot;


/**
 * @author LoMu
 * Date  2023-01-10 19:10
 */

@Mapper(componentModel = "spring")
public interface CopilotMapper {

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


    CopilotInfo toCopilotInfo(Copilot copilot);
}
