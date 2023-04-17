package plus.maa.backend.common.utils.converter;

import java.util.Objects;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import plus.maa.backend.controller.response.user.MaaUserInfo;
import plus.maa.backend.repository.entity.MaaUser;

/**
 * @author dragove
 * created on 2022/12/26
 */
@Mapper(imports = {
        Objects.class
})
public interface MaaUserConverter {

    MaaUserConverter INSTANCE = Mappers.getMapper(MaaUserConverter.class);

    @Mapping(source = "userId", target = "id")
    @Mapping(target = "activated", expression = "java(Objects.equals(user.getStatus(), 1))")
    @Mapping(target = "uploadCount", ignore = true)
    MaaUserInfo convert(MaaUser user);

}
