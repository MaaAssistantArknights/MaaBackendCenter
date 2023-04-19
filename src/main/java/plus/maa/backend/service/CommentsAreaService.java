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
import plus.maa.backend.repository.entity.CopilotRating;
import plus.maa.backend.repository.entity.MaaUser;

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

        Assert.isTrue(StringUtils.isNotBlank(message), "评论不可为空");
        Assert.isTrue(copilotRepository.existsCopilotsByCopilotId(copilotId), "作业表不存在");


        String fromCommentsId = null;
        String mainCommentsId = null;

        if (StringUtils.isNoneBlank(commentsAddDTO.getFromCommentId())) {

            Optional<CommentsArea> commentsAreaOptional = commentsAreaRepository.findById(commentsAddDTO.getFromCommentId());
            Assert.isTrue(commentsAreaOptional.isPresent(), "回复的评论不存在");
            CommentsArea rawCommentsArea = commentsAreaOptional.get();

            //判断其回复的评论是主评论 还是子评论
            mainCommentsId = StringUtils
                    .isNoneBlank(rawCommentsArea.getMainCommentId()) ?
                    rawCommentsArea.getMainCommentId() : rawCommentsArea.getId();

            fromCommentsId = StringUtils
                    .isNoneBlank(rawCommentsArea.getId()) ?
                    rawCommentsArea.getId() : null;

        }

        //创建评论表
        CommentsArea commentsArea = new CommentsArea();
        commentsArea.setCopilotId(copilotId)
                .setUploaderId(userId)
                .setFromCommentId(fromCommentsId)
                .setMainCommentId(mainCommentsId)
                .setMessage(message);
        commentsAreaRepository.insert(commentsArea);

    }


    public void deleteComments(String userId, String commentsId) {
        CommentsArea commentsArea = findCommentsById(commentsId);
        verifyOwner(userId, commentsArea.getUploaderId());

        Date date = new Date();
        commentsArea.setDelete(true);
        commentsArea.setDeleteTime(date);

        //删除所有回复
        if (StringUtils.isBlank(commentsArea.getMainCommentId())) {
            List<CommentsArea> commentsAreaList = commentsAreaRepository.findByMainCommentId(commentsArea.getId());
            commentsAreaList.forEach(ca ->
                    ca.setDeleteTime(date)
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
                ratingUser.setRating(rating);
                existRatingUser = true;
            }
        }

        //不存在 创建一个用户评分
        if (!existRatingUser) {
            CopilotRating.RatingUser ratingUser = new CopilotRating.RatingUser(userId, rating);
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
        Page<CommentsArea> mainCommentsList = commentsAreaRepository.findByCopilotIdAndDeleteAndMainCommentIdExists(request.getCopilotId(), false, false, pageable);
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


    private void verifyOwner(String userId, String uploaderId) {
        Assert.isTrue(Objects.equals(userId, uploaderId), "您无法删除不属于您的评论");
    }


    private CommentsArea findCommentsById(String commentsId) {
        Optional<CommentsArea> commentsArea = commentsAreaRepository.findById(commentsId);
        Assert.isTrue(commentsArea.isPresent(), "评论不存在");
        return commentsArea.get();
    }

}
