package plus.maa.backend.repository.entity;


import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import plus.maa.backend.controller.request.CopilotDTO;


/**
 * @author LoMu
 * Date  2023-01-10 19:10
 */

@Mapper(componentModel = "spring")
public interface CopilotMapper {

    /**
     * 实现增量更新
     * 将copilot 映射覆盖数据库中的 rawCopilot
     * 映射中跳过空值
     *
     * @param copilotDTO 更新值
     * @param rawCopilot 从数据库中查出的原始值
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateCopilotToRaw(CopilotDTO copilotDTO, @MappingTarget Copilot rawCopilot);


    Copilot toCopilot(CopilotDTO copilotDto);
}
