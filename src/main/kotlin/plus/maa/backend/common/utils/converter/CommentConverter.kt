package plus.maa.backend.common.utils.converter

import org.mapstruct.Mapper
import org.mapstruct.Mapping
import plus.maa.backend.controller.response.comments.CommentsInfo
import plus.maa.backend.controller.response.comments.SubCommentsInfo
import plus.maa.backend.repository.entity.CommentsArea
import plus.maa.backend.repository.entity.MaaUser

/**
 * @author LoMu
 * Date  2023-02-21 18:16
 */
@Mapper(componentModel = "spring")
interface CommentConverter {
    @Mapping(target = "like", source = "commentsArea.likeCount")
    @Mapping(target = "dislike", source = "commentsArea.dislikeCount")
    @Mapping(target = "uploader", source = "maaUser.userName")
    @Mapping(target = "commentId", source = "commentsArea.id")
    fun toCommentsInfo(commentsArea: CommentsArea, maaUser: MaaUser, subCommentsInfos: List<SubCommentsInfo>): CommentsInfo


    @Mapping(target = "like", source = "commentsArea.likeCount")
    @Mapping(target = "dislike", source = "commentsArea.dislikeCount")
    @Mapping(target = "uploader", source = "maaUser.userName")
    @Mapping(target = "commentId", source = "commentsArea.id")
    @Mapping(target = "deleted", source = "commentsArea.delete")
    fun toSubCommentsInfo(commentsArea: CommentsArea, maaUser: MaaUser): SubCommentsInfo
}
