package plus.maa.backend.common.utils.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import plus.maa.backend.controller.response.ArkLevelInfo;
import plus.maa.backend.repository.entity.ArkLevel;

/**
 * @author dragove
 * created on 2022/12/26
 */
@Mapper
public interface ArkLevelConverter {

    ArkLevelConverter INSTANCE = Mappers.getMapper(ArkLevelConverter.class);

    @Mapping(target = "custom", ignore = true)
    ArkLevelInfo convert(ArkLevel arkLevel);

}
