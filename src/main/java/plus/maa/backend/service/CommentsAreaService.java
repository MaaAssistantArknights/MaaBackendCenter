package plus.maa.backend.service;


import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import plus.maa.backend.common.utils.converter.CommentConverter;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.controller.request.comments.CommentsAddDTO;
import plus.maa.backend.controller.request.comments.CommentsQueriesDTO;
import plus.maa.backend.controller.request.comments.CommentsRatingDTO;
import plus.maa.backend.controller.request.comments.CommentsToppingDTO;
import plus.maa.backend.controller.response.comments.CommentsAreaInfo;
import plus.maa.backend.controller.response.comments.CommentsInfo;
import plus.maa.backend.controller.response.comments.SubCommentsInfo;
import plus.maa.backend.repository.CommentsAreaRepository;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.RatingRepository;
import plus.maa.backend.repository.UserRepository;
import plus.maa.backend.repository.entity.CommentsArea;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.repository.entity.Rating;
import plus.maa.backend.service.model.CommentNotification;
import plus.maa.backend.service.model.RatingType;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * @author LoMu
 * Date  2023-02-17 15:00
 */

@Service
@RequiredArgsConstructor
public class CommentsAreaService {
    private final CommentsAreaRepository commentsAreaRepository;

    private final RatingRepository ratingRepository;

    private final CopilotRepository copilotRepository;

    private final UserRepository userRepository;

    private final EmailService emailService;

    private final MaaCopilotProperties maaCopilotProperties;

    private final MaaUser UNKNOWN_USER = new MaaUser().setUserName("未知用户:(");

    private final CommentConverter commentConverter;


    /**
     * 评论
     * 每个评论都有一个uuid加持
     *
     * @param userId         登录用户 id
     * @param commentsAddDTO CommentsRequest
     */
    public void addComments(String userId, CommentsAddDTO commentsAddDTO) {
        long copilotId = Long.parseLong(commentsAddDTO.getCopilotId());
        String message = commentsAddDTO.getMessage();
        Optional<Copilot> copilotOptional = copilotRepository.findByCopilotId(copilotId);
        Assert.isTrue(StringUtils.isNotBlank(message), "评论不可为空");
        Assert.isTrue(copilotOptional.isPresent(), "作业表不存在");


        String fromCommentsId = null;
        String mainCommentsId = null;

        CommentsArea commentsArea = null;
        Boolean isCopilotAuthor = null;


        //代表这是一条回复评论
        if (StringUtils.isNoneBlank(commentsAddDTO.getFromCommentId())) {

            Optional<CommentsArea> commentsAreaOptional = commentsAreaRepository.findById(commentsAddDTO.getFromCommentId());
            Assert.isTrue(commentsAreaOptional.isPresent(), "回复的评论不存在");
            commentsArea = commentsAreaOptional.get();
            Assert.isTrue(!commentsArea.isDelete(), "回复的评论不存在");

            mainCommentsId = StringUtils
                    .isNoneBlank(commentsArea.getMainCommentId()) ?
                    commentsArea.getMainCommentId() : commentsArea.getId();

            fromCommentsId = StringUtils
                    .isNoneBlank(commentsArea.getId()) ?
                    commentsArea.getId() : null;

            if (Objects.isNull(commentsArea.getNotification()) || commentsArea.getNotification()) {
                isCopilotAuthor = false;
            }

        } else {
            isCopilotAuthor = true;
        }

        //判断是否需要通知
        if (Objects.nonNull(isCopilotAuthor) && maaCopilotProperties.getMail().getNotification()) {
            Copilot copilot = copilotOptional.get();

            //通知作业作者或是评论作者
            String replyUserId = isCopilotAuthor ? copilot.getUploaderId() : commentsArea.getUploaderId();


            Map<String, MaaUser> maaUserMap = userRepository.findByUsersId(List.of(userId, replyUserId));

            //防止通知自己
            if (!Objects.equals(replyUserId, userId)) {
                LocalDateTime time = LocalDateTime.now();
                String timeStr = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                CommentNotification commentNotification = new CommentNotification();


                String authorName = maaUserMap.getOrDefault(replyUserId, UNKNOWN_USER).getUserName();
                String reName = maaUserMap.getOrDefault(userId, UNKNOWN_USER).getUserName();

                String title = isCopilotAuthor ? copilot.getDoc().getTitle() : commentsArea.getMessage();

                commentNotification
                        .setTitle(title)
                        .setDate(timeStr)
                        .setAuthorName(authorName)
                        .setReName(reName)
                        .setReMessage(message);


                MaaUser maaUser = maaUserMap.get(replyUserId);
                if (Objects.nonNull(maaUser)) {
                    emailService.sendCommentNotification(maaUser.getEmail(), commentNotification);
                }
            }
        }


        //创建评论表
        commentsAreaRepository.insert(
                new CommentsArea().setCopilotId(copilotId)
                        .setUploaderId(userId)
                        .setFromCommentId(fromCommentsId)
                        .setMainCommentId(mainCommentsId)
                        .setMessage(message)
                        .setNotification(commentsAddDTO.isNotification())
        );

    }


    public void deleteComments(String userId, String commentsId) {
        CommentsArea commentsArea = findCommentsById(commentsId);
        //允许作者删除评论
        copilotRepository.findByCopilotId(commentsArea.getCopilotId())
                .ifPresent(copilot ->
                        Assert.isTrue(
                                Objects.equals(userId, copilot.getUploaderId())
                                        || Objects.equals(userId, commentsArea.getUploaderId()),
                                "您无法删除不属于您的评论")
                );
        LocalDateTime now = LocalDateTime.now();
        commentsArea.setDelete(true);
        commentsArea.setDeleteTime(now);

        //删除所有回复
        if (StringUtils.isBlank(commentsArea.getMainCommentId())) {
            List<CommentsArea> commentsAreaList = commentsAreaRepository.findByMainCommentId(commentsArea.getId());
            commentsAreaList.forEach(ca ->
                    ca.setDeleteTime(now)
                            .setDelete(true)
            );
            commentsAreaRepository.saveAll(commentsAreaList);
        }
        commentsAreaRepository.save(commentsArea);
    }


    /**
     * 为评论进行点赞
     *
     * @param userId            登录用户 id
     * @param commentsRatingDTO CommentsRatingDTO
     */
    public void rates(String userId, CommentsRatingDTO commentsRatingDTO) {
        String rating = commentsRatingDTO.getRating();

        CommentsArea commentsArea = findCommentsById(commentsRatingDTO.getCommentId());

        long change;
        Optional<Rating> ratingOptional = ratingRepository.findByTypeAndKeyAndUserId(Rating.KeyType.COMMENT, commentsArea.getId(), userId);
        // 判断该用户是否存在评分
        if (ratingOptional.isPresent()) {
            // 如果评分发生变化则更新
            if (!Objects.equals(ratingOptional.get().getRating(), RatingType.fromRatingType(rating))) {
                RatingType oldRatingType = ratingOptional.get().getRating();
                ratingOptional.get().setRating(RatingType.fromRatingType(rating));
                ratingOptional.get().setRateTime(LocalDateTime.now());
                RatingType newRatingType = ratingRepository.save(ratingOptional.get()).getRating();
                // 更新评分后更新评论的点赞数
                change = newRatingType == RatingType.LIKE ? 1 :
                        (oldRatingType != RatingType.LIKE ? 0 : -1);
            } else {
                // 如果评分未发生变化则结束
                return;
            }
        } else {
            // 不存在评分则创建
            Rating newRating = new Rating()
                    .setType(Rating.KeyType.COMMENT)
                    .setKey(commentsArea.getId())
                    .setUserId(userId)
                    .setRating(RatingType.fromRatingType(rating))
                    .setRateTime(LocalDateTime.now());

            ratingRepository.insert(newRating);
            change = newRating.getRating() == RatingType.LIKE ? 1 : 0;
        }

        // 点赞数不需要在高并发下特别精准，大概就行，但是也得避免特别离谱的数字
        long likeCount = commentsArea.getLikeCount() + change;
        if (likeCount < 0) {
            likeCount = 0;
        }
        commentsArea.setLikeCount(likeCount);

        commentsAreaRepository.save(commentsArea);
    }

    /**
     * 评论置顶
     *
     * @param userId             登录用户 id
     * @param commentsToppingDTO CommentsToppingDTO
     */
    public void topping(String userId, CommentsToppingDTO commentsToppingDTO) {
        CommentsArea commentsArea = findCommentsById(commentsToppingDTO.getCommentId());
        Assert.isTrue(!commentsArea.isDelete(), "评论不存在");
        // 只允许作者置顶评论
        copilotRepository.findByCopilotId(commentsArea.getCopilotId())
                .ifPresent(copilot -> {
                            Assert.isTrue(
                                    Objects.equals(userId, copilot.getUploaderId()),
                                    "只有作者才能置顶评论");
                            commentsArea.setTopping(commentsToppingDTO.isTopping());
                            commentsAreaRepository.save(commentsArea);
                        }
                );
    }

    /**
     * 查询
     *
     * @param request CommentsQueriesDTO
     * @return CommentsAreaInfo
     */
    public CommentsAreaInfo queriesCommentsArea(CommentsQueriesDTO request) {
        Sort.Order toppingOrder = Sort.Order.desc("topping");

        Sort.Order sortOrder = new Sort.Order(
                request.isDesc() ? Sort.Direction.DESC : Sort.Direction.ASC,
                Optional.ofNullable(request.getOrderBy())
                        .filter(StringUtils::isNotBlank)
                        .map(ob -> switch (ob) {
                            case "hot" -> "likeCount";
                            case "id" -> "uploadTime";
                            default -> request.getOrderBy();
                        }).orElse("likeCount"));

        int page = request.getPage() > 0 ? request.getPage() : 1;
        int limit = request.getLimit() > 0 ? request.getLimit() : 10;


        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(toppingOrder, sortOrder));


        //主评论

        Page<CommentsArea> mainCommentsList;

        if (StringUtils.isNotBlank(request.getJustSeeId())) {
            mainCommentsList = commentsAreaRepository.findByCopilotIdAndUploaderIdAndDeleteAndMainCommentIdExists(request.getCopilotId(), request.getJustSeeId(), false, false, pageable);
        } else {
            mainCommentsList = commentsAreaRepository.findByCopilotIdAndDeleteAndMainCommentIdExists(request.getCopilotId(), false, false, pageable);
        }

        long count = mainCommentsList.getTotalElements();

        int pageNumber = mainCommentsList.getTotalPages();

        // 判断是否存在下一页
        boolean hasNext = count - (long) page * limit > 0;


        //获取子评论
        List<CommentsArea> subCommentsList = commentsAreaRepository.findByMainCommentIdIn(
                mainCommentsList.stream()
                        .map(CommentsArea::getId)
                        .toList()
        );

        //将已删除评论内容替换为空
        subCommentsList.forEach(comment -> {
            if (comment.isDelete()) {
                comment.setMessage("");
            }
        });


        //所有评论
        List<CommentsArea> allComments = new ArrayList<>(mainCommentsList.stream().toList());
        allComments.addAll(subCommentsList);

        //获取所有评论用户
        List<String> userId = allComments.stream().map(CommentsArea::getUploaderId).distinct().toList();
        Map<String, MaaUser> maaUserMap = userRepository.findByUsersId(userId);


        //转换主评论数据并填充用户名
        List<CommentsInfo> commentsInfos = mainCommentsList.stream().map(mainComment -> {
            CommentsInfo commentsInfo =
                    commentConverter
                            .toCommentsInfo(
                                    mainComment
                                    , mainComment.getId()
                                    , (int) mainComment.getLikeCount()
                                    , maaUserMap.getOrDefault(
                                            mainComment.getUploaderId()
                                            , UNKNOWN_USER
                                    )
                            );

            List<SubCommentsInfo> subCommentsInfoList = subCommentsList.stream()
                    .filter(comment -> Objects.equals(commentsInfo.getCommentId(), comment.getMainCommentId()))
                    //转换子评论数据并填充用户名
                    .map(subComment ->
                            commentConverter
                                    .toSubCommentsInfo(
                                            subComment
                                            , subComment.getId()
                                            , (int) subComment.getLikeCount()
                                            //填充评论用户名
                                            , maaUserMap.getOrDefault(
                                                    subComment.getUploaderId(),
                                                    UNKNOWN_USER
                                            )
                                            , subComment.isDelete()
                                    )
                    ).toList();


            commentsInfo.setSubCommentsInfos(subCommentsInfoList);
            return commentsInfo;
        }).toList();

        return new CommentsAreaInfo().setHasNext(hasNext)
                .setPage(pageNumber)
                .setTotal(count)
                .setData(commentsInfos);
    }


    private CommentsArea findCommentsById(String commentsId) {
        Optional<CommentsArea> commentsArea = commentsAreaRepository.findById(commentsId);
        Assert.isTrue(commentsArea.isPresent(), "评论不存在");
        return commentsArea.get();
    }


    public void notificationStatus(String userId, String id, boolean status) {
        Optional<CommentsArea> commentsAreaOptional = commentsAreaRepository.findById(id);
        Assert.isTrue(commentsAreaOptional.isPresent(), "评论不存在");
        CommentsArea commentsArea = commentsAreaOptional.get();
        Assert.isTrue(Objects.equals(userId, commentsArea.getUploaderId()), "您没有权限修改");
        commentsArea.setNotification(status);
        commentsAreaRepository.save(commentsArea);
    }
}
