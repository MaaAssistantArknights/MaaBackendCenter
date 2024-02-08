package plus.maa.backend.common.utils.converter

import org.mapstruct.Mapper
import plus.maa.backend.controller.response.copilot.ArkLevelInfo
import plus.maa.backend.repository.entity.ArkLevel

/**
 * @author dragove
 * created on 2022/12/26
 */
@Mapper(componentModel = "spring")
interface ArkLevelConverter {
    fun convert(arkLevel: ArkLevel): ArkLevelInfo

    fun convert(arkLevel: List<ArkLevel>): List<ArkLevelInfo>
}
