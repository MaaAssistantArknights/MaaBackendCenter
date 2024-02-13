package plus.maa.backend.service

import lombok.RequiredArgsConstructor
import org.apache.commons.lang3.StringUtils
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import plus.maa.backend.common.utils.converter.CommentConverter
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.controller.request.comments.CommentsAddDTO
import plus.maa.backend.controller.request.comments.CommentsQueriesDTO
import plus.maa.backend.controller.request.comments.CommentsRatingDTO
import plus.maa.backend.controller.request.comments.CommentsToppingDTO
import plus.maa.backend.controller.response.comments.CommentsAreaInfo
import plus.maa.backend.repository.*
import plus.maa.backend.repository.entity.CommentsArea
import plus.maa.backend.repository.entity.Copilot
import plus.maa.backend.repository.entity.MaaUser
import plus.maa.backend.repository.entity.Rating
import plus.maa.backend.service.model.CommentNotification
import plus.maa.backend.service.model.RatingType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.collections.ArrayList

/**
 * @author LoMu
 * Date  2023-02-17 15:00
 */
@Service
@RequiredArgsConstructor
class CommentsAreaService(
    private val commentsAreaRepository: CommentsAreaRepository,
    private val ratingRepository: RatingRepository,
    private val copilotRepository: CopilotRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService,
    private val maaCopilotProperties: MaaCopilotProperties,
    private val commentConverter: CommentConverter,
) {


    /**
     * 评论
     * 每个评论都有一个uuid加持
     *
     * @param userId         登录用户 id
     * @param commentsAddDTO CommentsRequest
     */
    fun addComments(userId: String, commentsAddDTO: CommentsAddDTO) {
        val copilotId = commentsAddDTO.copilotId.toLong()
        val message = commentsAddDTO.message
        val copilotOptional = copilotRepository.findByCopilotId(copilotId)
        Assert.isTrue(StringUtils.isNotBlank(message), "评论不可为空")
        Assert.isTrue(copilotOptional != null, "作业表不存在")


        var fromCommentsId: String? = null
        var mainCommentsId: String? = null

        var commentsArea: CommentsArea? = null
        var isCopilotAuthor: Boolean? = null


        //代表这是一条回复评论
        if (!commentsAddDTO.fromCommentId.isNullOrBlank()) {
            val commentsAreaOptional = commentsAreaRepository.findById(commentsAddDTO.fromCommentId)
            Assert.isTrue(commentsAreaOptional.isPresent, "回复的评论不存在")
            commentsArea = commentsAreaOptional.get()
            Assert.isTrue(!commentsArea.isDelete, "回复的评论不存在")

            mainCommentsId = if (StringUtils
                    .isNoneBlank(commentsArea.mainCommentId)
            ) commentsArea.mainCommentId else commentsArea.id

            fromCommentsId = if (StringUtils
                    .isNoneBlank(commentsArea.id)
            ) commentsArea.id else null

            if (Objects.isNull(commentsArea.notification) || commentsArea.notification) {
                isCopilotAuthor = false
            }
        } else {
            isCopilotAuthor = true
        }

        //判断是否需要通知
        if (Objects.nonNull(isCopilotAuthor) && maaCopilotProperties.mail.notification) {
            val copilot = copilotOptional!!

            //通知作业作者或是评论作者
            val replyUserId = if (isCopilotAuthor!!) copilot.uploaderId else commentsArea!!.uploaderId


            val maaUserMap = userRepository.findByUsersId(listOf(userId, replyUserId))

            //防止通知自己
            if (replyUserId != userId) {
                val time = LocalDateTime.now()
                val timeStr = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val commentNotification = CommentNotification()


                val authorName = maaUserMap.getOrDefault(replyUserId, MaaUser.UNKNOWN).userName
                val reName = maaUserMap.getOrDefault(userId, MaaUser.UNKNOWN).userName

                val title = if (isCopilotAuthor) copilot.doc.title else commentsArea!!.message

                commentNotification
                    .setTitle(title)
                    .setDate(timeStr)
                    .setAuthorName(authorName)
                    .setReName(reName)
                    .setReMessage(message)


                val maaUser = maaUserMap[replyUserId]
                if (Objects.nonNull(maaUser)) {
                    emailService.sendCommentNotification(maaUser!!.email, commentNotification)
                }
            }
        }


        //创建评论表
        commentsAreaRepository.insert(
            CommentsArea().setCopilotId(copilotId)
                .setUploaderId(userId)
                .setFromCommentId(fromCommentsId)
                .setMainCommentId(mainCommentsId)
                .setMessage(message)
                .setNotification(commentsAddDTO.notification)
        )
    }


    fun deleteComments(userId: String, commentsId: String) {
        val commentsArea = findCommentsById(commentsId)
        //允许作者删除评论
        copilotRepository.findByCopilotId(commentsArea.copilotId)?.let { copilot: Copilot ->
            Assert.isTrue(
                userId == copilot.uploaderId || userId == commentsArea.uploaderId,
                "您无法删除不属于您的评论"
            )
        }
        val now = LocalDateTime.now()
        commentsArea.setDelete(true)
        commentsArea.setDeleteTime(now)

        //删除所有回复
        if (StringUtils.isBlank(commentsArea.mainCommentId)) {
            val commentsAreaList = commentsAreaRepository.findByMainCommentId(commentsArea.id)
            commentsAreaList.forEach{ ca: CommentsArea ->
                    ca.setDeleteTime(now)
                        .setDelete(true)
                }
            commentsAreaRepository.saveAll(commentsAreaList)
        }
        commentsAreaRepository.save(commentsArea)
    }


    /**
     * 为评论进行点赞
     *
     * @param userId            登录用户 id
     * @param commentsRatingDTO CommentsRatingDTO
     */
    fun rates(userId: String, commentsRatingDTO: CommentsRatingDTO) {
        val rating = commentsRatingDTO.rating

        val commentsArea = findCommentsById(commentsRatingDTO.commentId)

        val likeCountChange: Long
        val dislikeCountChange: Long

        val ratingOptional =
            ratingRepository.findByTypeAndKeyAndUserId(Rating.KeyType.COMMENT, commentsArea.id, userId)
        // 判断该用户是否存在评分
        if (ratingOptional != null) {
            // 如果评分发生变化则更新
            if (ratingOptional.rating != RatingType.fromRatingType(rating)) {
                val oldRatingType = ratingOptional.rating
                ratingOptional.rating = RatingType.fromRatingType(rating)
                ratingOptional.rateTime = LocalDateTime.now()
                val newRatingType = ratingRepository.save(ratingOptional).rating
                // 更新评分后更新评论的点赞数
                likeCountChange =
                    (if (newRatingType == RatingType.LIKE) 1 else (if (oldRatingType != RatingType.LIKE) 0 else -1)).toLong()
                dislikeCountChange =
                    (if (newRatingType == RatingType.DISLIKE) 1 else (if (oldRatingType != RatingType.DISLIKE) 0 else -1)).toLong()
            } else {
                // 如果评分未发生变化则结束
                return
            }
        } else {
            // 不存在评分则创建
            val newRating = Rating(
                null,
                Rating.KeyType.COMMENT,
                commentsArea.id,
                userId,
                RatingType.fromRatingType(rating),
                LocalDateTime.now()
            )

            ratingRepository.insert(newRating)
            likeCountChange = (if (newRating.rating == RatingType.LIKE) 1 else 0).toLong()
            dislikeCountChange = (if (newRating.rating == RatingType.DISLIKE) 1 else 0).toLong()
        }

        // 点赞数不需要在高并发下特别精准，大概就行，但是也得避免特别离谱的数字
        var likeCount = commentsArea.likeCount + likeCountChange
        if (likeCount < 0) {
            likeCount = 0
        }

        var dislikeCount = commentsArea.dislikeCount + dislikeCountChange
        if (dislikeCount < 0) {
            dislikeCount = 0
        }

        commentsArea.setLikeCount(likeCount)
        commentsArea.setDislikeCount(dislikeCount)

        commentsAreaRepository.save(commentsArea)
    }

    /**
     * 评论置顶
     *
     * @param userId             登录用户 id
     * @param commentsToppingDTO CommentsToppingDTO
     */
    fun topping(userId: String, commentsToppingDTO: CommentsToppingDTO) {
        val commentsArea = findCommentsById(commentsToppingDTO.commentId)
        Assert.isTrue(!commentsArea.isDelete, "评论不存在")
        // 只允许作者置顶评论
        copilotRepository.findByCopilotId(commentsArea.copilotId)?.let { copilot: Copilot ->
            Assert.isTrue(
                userId == copilot.uploaderId,
                "只有作者才能置顶评论"
            )
            commentsArea.setTopping(commentsToppingDTO.topping)
            commentsAreaRepository.save(commentsArea)
        }
    }

    /**
     * 查询
     *
     * @param request CommentsQueriesDTO
     * @return CommentsAreaInfo
     */
    fun queriesCommentsArea(request: CommentsQueriesDTO): CommentsAreaInfo {
        val toppingOrder = Sort.Order.desc("topping")

        val sortOrder = Sort.Order(
            if (request.desc) Sort.Direction.DESC else Sort.Direction.ASC,
            when (request.orderBy) {
                "hot" -> "likeCount"
                "id" -> "uploadTime"
                else -> request.orderBy ?: "likeCount"
            }
        )

        val page = if (request.page > 0) request.page else 1
        val limit = if (request.limit > 0) request.limit else 10


        val pageable: Pageable = PageRequest.of(page - 1, limit, Sort.by(toppingOrder, sortOrder))


        //主评论
        val mainCommentsList = if (!request.justSeeId.isNullOrBlank()) {
            commentsAreaRepository.findByCopilotIdAndUploaderIdAndDeleteAndMainCommentIdExists(
                request.copilotId,
                request.justSeeId,
                delete = false,
                exists = false,
                pageable = pageable
            )
        } else {
            commentsAreaRepository.findByCopilotIdAndDeleteAndMainCommentIdExists(
                request.copilotId,
                delete = false,
                exists = false,
                pageable = pageable
            )
        }

        val count = mainCommentsList.totalElements

        val pageNumber = mainCommentsList.totalPages

        // 判断是否存在下一页
        val hasNext = count - page.toLong() * limit > 0


        //获取子评论
        val subCommentsList = commentsAreaRepository.findByMainCommentIdIn(
            mainCommentsList
                .map { obj: CommentsArea -> obj.id }
                .toList()
        )

        //将已删除评论内容替换为空
        subCommentsList.forEach{ comment: CommentsArea ->
            if (comment.isDelete) {
                comment.setMessage("")
            }
        }


        //所有评论
        val allComments: MutableList<CommentsArea> = ArrayList(mainCommentsList.toList())
        allComments.addAll(subCommentsList)

        //获取所有评论用户
        val userIds = allComments.map { obj: CommentsArea -> obj.uploaderId }.distinct().toList()
        val maaUserMap = userRepository.findByUsersId(userIds)


        //转换主评论数据并填充用户名
        val commentsInfos = mainCommentsList.map { mainComment: CommentsArea ->
            val subCommentsInfoList = subCommentsList
                .filter { comment: CommentsArea -> mainComment.id == comment.mainCommentId } //转换子评论数据并填充用户名
                .map { subComment: CommentsArea ->
                    commentConverter.toSubCommentsInfo(
                        subComment,  //填充评论用户名
                        maaUserMap.getOrDefault(
                            subComment.uploaderId,
                            MaaUser.UNKNOWN
                        )
                    )
                }.toList()
            val commentsInfo = commentConverter.toCommentsInfo(
                mainComment,
                maaUserMap.getOrDefault(
                    mainComment.uploaderId,
                    MaaUser.UNKNOWN
                ),
                subCommentsInfoList
            )
            commentsInfo
        }.toList()

        return CommentsAreaInfo(hasNext, pageNumber, count, commentsInfos)
    }


    private fun findCommentsById(commentsId: String): CommentsArea {
        val commentsArea = commentsAreaRepository.findById(commentsId)
        Assert.isTrue(commentsArea.isPresent, "评论不存在")
        return commentsArea.get()
    }


    fun notificationStatus(userId: String, id: String, status: Boolean) {
        val commentsAreaOptional = commentsAreaRepository.findById(id)
        Assert.isTrue(commentsAreaOptional.isPresent, "评论不存在")
        val commentsArea = commentsAreaOptional.get()
        Assert.isTrue(userId == commentsArea.uploaderId, "您没有权限修改")
        commentsArea.setNotification(status)
        commentsAreaRepository.save(commentsArea)
    }
}
