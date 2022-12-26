package plus.maa.backend.common.utils.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import plus.maa.backend.controller.response.MaaUserInfo;
import plus.maa.backend.repository.entity.MaaUser;

/**
 * @author dragove
 * created on 2022/12/26
 */
@Mapper
public interface MaaUserConverter {

    MaaUserConverter INSTANCE = Mappers.getMapper(MaaUserConverter.class);

    @Mapping(target = "activated", ignore = true)
    @Mapping(target = "uploadCount", ignore = true)
    MaaUserInfo convert(MaaUser user);

}
