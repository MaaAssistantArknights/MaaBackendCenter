package plus.maa.backend.common.utils.converter;

import org.mapstruct.Mapper;

import plus.maa.backend.controller.response.copilot.ArkLevelInfo;
import plus.maa.backend.repository.entity.ArkLevel;

import java.util.List;

/**
 * @author dragove
 * created on 2022/12/26
 */
@Mapper(componentModel = "spring")
public interface ArkLevelConverter {

    ArkLevelInfo convert(ArkLevel arkLevel);

    List<ArkLevelInfo> convert(List<ArkLevel> arkLevel);

}
