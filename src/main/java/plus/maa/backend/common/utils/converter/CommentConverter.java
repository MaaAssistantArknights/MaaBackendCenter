package plus.maa.backend.common.utils.converter;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import plus.maa.backend.controller.response.comments.CommentsInfo;
import plus.maa.backend.controller.response.comments.SubCommentsInfo;
import plus.maa.backend.repository.entity.CommentsArea;
import plus.maa.backend.repository.entity.MaaUser;

/**
 * @author LoMu
 * Date  2023-02-21 18:16
 */

@Mapper(componentModel = "spring")
public interface CommentConverter {

    @Mapping(target = "like", source = "commentsArea.likeCount")
    @Mapping(target = "dislike", source = "commentsArea.dislikeCount")
    @Mapping(target = "uploader", source = "maaUser.userName")
    @Mapping(target = "commentId", source = "id")
    @Mapping(target = "subCommentsInfos", ignore = true)
    CommentsInfo toCommentsInfo(CommentsArea commentsArea, String id, MaaUser maaUser);


    @Mapping(target = "like", source = "commentsArea.likeCount")
    @Mapping(target = "dislike", source = "commentsArea.dislikeCount")
    @Mapping(target = "uploader", source = "maaUser.userName")
    @Mapping(target = "commentId", source = "id")
    @Mapping(target = "deleted", source = "delete")
    SubCommentsInfo toSubCommentsInfo(CommentsArea commentsArea, String id, MaaUser maaUser, boolean delete);
}
