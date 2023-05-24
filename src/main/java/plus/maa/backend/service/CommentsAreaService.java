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
import plus.maa.backend.controller.response.comments.CommentsAreaInfo;
import plus.maa.backend.controller.response.comments.CommentsInfo;
import plus.maa.backend.controller.response.comments.SubCommentsInfo;
import plus.maa.backend.repository.CommentsAreaRepository;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.UserRepository;
import plus.maa.backend.repository.entity.CommentsArea;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.model.CommentNotification;

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

    private final CopilotRepository copilotRepository;

    private final UserRepository userRepository;

    private final EmailService emailService;

    private final MaaCopilotProperties maaCopilotProperties;


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

        //通知
        if (Objects.nonNull(isCopilotAuthor) && maaCopilotProperties.getMail().getNotification()) {
            Copilot copilot = copilotOptional.get();
            //通知作业作者或是评论作者
            String replyUserId = isCopilotAuthor ? copilot.getUploaderId() : commentsArea.getUploaderId();
            String title = isCopilotAuthor ? copilot.getDoc().getTitle() : commentsArea.getMessage();
            Map<String, MaaUser> maaUserMap = userRepository.findByUsersId(List.of(userId, replyUserId));

            if (!Objects.equals(replyUserId, userId)) {
                LocalDateTime time = LocalDateTime.now();
                String timeStr = time.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                CommentNotification commentNotification = new CommentNotification();
                commentNotification
                        .setTitle(title)
                        .setDate(timeStr)
                        .setName(maaUserMap.getOrDefault(replyUserId, new MaaUser().setUserName("用户已注销:(")).getUserName())
                        .setReName(maaUserMap.getOrDefault(userId, new MaaUser().setUserName("用户已注销:(")).getUserName())
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
        boolean existRatingUser = false;

        CommentsArea commentsArea = findCommentsById(commentsRatingDTO.getCommentId());
        List<CopilotRating.RatingUser> ratingUserList = commentsArea.getRatingUser();

        //判断是否存在 如果已存在则修改评分
        for (CopilotRating.RatingUser ratingUser : ratingUserList) {
            if (Objects.equals(userId, ratingUser.getUserId())) {
                if (Objects.equals(rating, ratingUser.getRating())) {
                    return;
                }
                ratingUser.setRating(rating);
                existRatingUser = true;
                break;
            }
        }

        //不存在 创建一个用户评分
        if (!existRatingUser) {
            CopilotRating.RatingUser ratingUser = new CopilotRating.RatingUser(userId, rating, LocalDateTime.now());
            ratingUserList.add(ratingUser);
        }

        long likeCount = ratingUserList.stream()
                .filter(ratingUser ->
                        Objects.equals(ratingUser.getRating(), "Like"))
                .count();
        commentsArea.setRatingUser(ratingUserList);
        commentsArea.setLikeCount(likeCount);


        commentsAreaRepository.save(commentsArea);
    }


    /**
     * 查询
     *
     * @param request CommentsQueriesDTO
     * @return CommentsAreaInfo
     */
    public CommentsAreaInfo queriesCommentsArea(CommentsQueriesDTO request) {
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


        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(sortOrder));


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
                    CommentConverter.INSTANCE
                            .toCommentsInfo(
                                    mainComment
                                    , mainComment.getId()
                                    , (int) mainComment.getLikeCount()
                                    , maaUserMap.getOrDefault(
                                            mainComment.getUploaderId()
                                            , new MaaUser().setUserName("未知用户):")
                                    )
                            );

            List<SubCommentsInfo> subCommentsInfoList = subCommentsList.stream()
                    .filter(comment -> Objects.equals(commentsInfo.getCommentId(), comment.getMainCommentId()))
                    //转换子评论数据并填充用户名
                    .map(subComment ->
                            CommentConverter.INSTANCE
                                    .toSubCommentsInfo(
                                            subComment
                                            , subComment.getId()
                                            , (int) subComment.getLikeCount()
                                            //填充评论用户名
                                            , maaUserMap.getOrDefault(
                                                    subComment.getUploaderId(),
                                                    new MaaUser().setUserName("未知用户):")
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
