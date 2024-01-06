package plus.maa.backend.common.utils.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import plus.maa.backend.controller.request.copilotset.CopilotSetCreateReq;
import plus.maa.backend.controller.response.user.CopilotSetListRes;
import plus.maa.backend.repository.entity.CopilotSet;

import java.time.LocalDateTime;

/**
 * @author dragove
 * create on 2024-01-01
 */
@Mapper(componentModel = "spring", imports = {
        LocalDateTime.class
})
public interface CopilotSetConverter {

    @Mapping(target = "delete", ignore = true)
    @Mapping(target = "deleteTime", ignore = true)
    @Mapping(target = "copilotIds", expression = "java(createReq.getDistinctIdsAndCheck())")
    @Mapping(target = "createTime", expression = "java(LocalDateTime.now())")
    @Mapping(target = "updateTime", expression = "java(LocalDateTime.now())")
    CopilotSet convert(CopilotSetCreateReq createReq, long id, String creatorId);

    @Mapping(target = "creator", ignore = true)
    CopilotSetListRes convert(CopilotSet copilotSet, String creator);

}
