package plus.maa.backend.common.utils.converter;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

import plus.maa.backend.controller.response.copilot.ArkLevelInfo;
import plus.maa.backend.repository.entity.ArkLevel;

import java.util.List;

/**
 * @author dragove
 * created on 2022/12/26
 */
@Mapper
public interface ArkLevelConverter {

    ArkLevelConverter INSTANCE = Mappers.getMapper(ArkLevelConverter.class);

    ArkLevelInfo convert(ArkLevel arkLevel);

    List<ArkLevelInfo> convert(List<ArkLevel> arkLevel);

}
