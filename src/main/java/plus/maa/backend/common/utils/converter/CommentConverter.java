package plus.maa.backend.common.utils.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import plus.maa.backend.controller.response.CommentsInfo;
import plus.maa.backend.controller.response.SubCommentsInfo;
import plus.maa.backend.repository.entity.CommentsArea;
import plus.maa.backend.repository.entity.MaaUser;

/**
 * @author LoMu
 * Date  2023-02-21 18:16
 */

@Mapper
public interface CommentConverter {
    CommentConverter INSTANCE = Mappers.getMapper(CommentConverter.class);


    @Mapping(target = "like", source = "likeCount")
    @Mapping(target = "uploader", source = "maaUser.userName")
    @Mapping(target = "commentId", source = "id")
    @Mapping(target = "subCommentsInfos", ignore = true)
    CommentsInfo toCommentsInfo(CommentsArea commentsArea, String id, int likeCount, MaaUser maaUser);


    @Mapping(target = "like", source = "likeCount")
    @Mapping(target = "uploader", source = "maaUser.userName")
    @Mapping(target = "commentId", source = "id")
    @Mapping(target = "deleted", source = "delete")
    SubCommentsInfo toSubCommentsInfo(CommentsArea commentsArea, String id, int likeCount, MaaUser maaUser, boolean delete);
}
