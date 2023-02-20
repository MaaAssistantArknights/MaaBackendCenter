package plus.maa.backend.service;


import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import plus.maa.backend.controller.request.CommentsAddDTO;
import plus.maa.backend.controller.request.CommentsQueriesDTO;
import plus.maa.backend.controller.request.CommentsRatingDTO;
import plus.maa.backend.controller.response.CommentsAreaInfo;
import plus.maa.backend.controller.response.CommentsInfo;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.SubCommentsInfo;
import plus.maa.backend.repository.CommentsAreaRepository;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.TableLogicDelete;
import plus.maa.backend.repository.entity.CommentsArea;
import plus.maa.backend.repository.entity.CopilotRating;
import plus.maa.backend.repository.entity.MaaUser;
import plus.maa.backend.service.model.LoginUser;

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

    private final MongoTemplate mongoTemplate;

    private final TableLogicDelete tableLogicDelete;


    /**
     * 评论
     * 每个评论都有一个uuid加持
     *
     * @param loginUser      登录用户
     * @param commentsAddDTO CommentsRequest
     * @return 评论 成功/失败
     */
    public MaaResult<String> addComments(LoginUser loginUser, CommentsAddDTO commentsAddDTO) {
        long copilotId = Long.parseLong(commentsAddDTO.getCopilotId());
        MaaUser maaUser = loginUser.getMaaUser();
        String message = commentsAddDTO.getMessage();

        Assert.isTrue(StringUtils.isNotBlank(message), "评论不可为空");
        Assert.isTrue(copilotRepository.existsCopilotsByCopilotId(copilotId), "作业表不存在");

        CommentsArea rawCommentsArea = commentsAreaRepository.findById(commentsAddDTO.getFromCommentsId()).orElse(new CommentsArea());

        String commentsId = StringUtils
                .isNoneBlank(rawCommentsArea.getMainCommentsId()) ?
                rawCommentsArea.getMainCommentsId() : rawCommentsArea.getId();

        String fromCommentsId = StringUtils
                .isNoneBlank(rawCommentsArea.getId()) ?
                rawCommentsArea.getId() : null;

        //创建评论表
        CommentsArea commentsArea = new CommentsArea();
        commentsArea.setCopilotId(copilotId)
                .setUploaderId(maaUser.getUserId())
                .setUploader(maaUser.getUserName())
                .setFromCommentsId(fromCommentsId)
                .setMainCommentsId(commentsId)
                .setMessage(message);
        commentsAreaRepository.insert(commentsArea);

        return MaaResult.success("评论成功");
    }


    public MaaResult<String> deleteComments(LoginUser loginUser, String commentsId) {
        CommentsArea commentsArea = findCommentsById(commentsId);
        verifyOwner(loginUser, commentsArea.getUploaderId());

        tableLogicDelete.deleteCommentsId(commentsId);
        return MaaResult.success("评论已删除");
    }


    /**
     * 为评论进行点赞
     *
     * @param loginUser         登录用户
     * @param commentsRatingDTO CommentsRatingDTO
     * @return String
     */
    public MaaResult<String> rates(LoginUser loginUser, CommentsRatingDTO commentsRatingDTO) {
        String userId = loginUser.getMaaUser().getUserId();
        String rating = commentsRatingDTO.getRating();
        boolean existRatingUser = false;

        CommentsArea commentsArea = findCommentsById(commentsRatingDTO.getCommentsId());
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
        return MaaResult.success("成功");
    }


    /**
     * 查询
     *
     * @param request CommentsQueriesDTO
     * @return CommentsAreaInfo
     */
    public MaaResult<CommentsAreaInfo> queriesCommentsArea(CommentsQueriesDTO request) {
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

        boolean hasNext = false;
        Pageable pageable = PageRequest.of(page - 1, limit, Sort.by(sortOrder));

        Query query = new Query();
        query.addCriteria(
                Criteria.where("copilotId").is(request.getCopilotId())
                        .and("delete").is(true));

        List<CommentsArea> commentsAreaList = mongoTemplate.find(query.with(pageable), CommentsArea.class);

        long count = mongoTemplate.count(query, CommentsArea.class);

        // 计算页面
        int pageNumber = (int) Math.ceil((double) count / limit);

        // 判断是否存在下一页
        if (count - (long) page * limit > 0) {
            hasNext = true;
        }

        CommentsAreaInfo commentsAreaInfo = new CommentsAreaInfo();
        List<CommentsInfo> commentsInfoList = new ArrayList<>();
        List<SubCommentsInfo> subCommentsInfoList = new ArrayList<>();


        commentsInfoList.add(new CommentsInfo().setSubCommentsInfos(subCommentsInfoList));
        commentsAreaInfo.setHasNext(hasNext)
                .setPage(pageNumber)
                .setTotal(count)
                .setData(commentsInfoList);

        System.out.println(commentsAreaList);
        return MaaResult.success(commentsAreaInfo);
    }


    private void verifyOwner(LoginUser user, String uploaderId) {
        Assert.isTrue(Objects.equals(user.getMaaUser().getUserId(), uploaderId), "您无法删除不属于您的评论");
    }


    private CommentsArea findCommentsById(String commentsId) {
        Optional<CommentsArea> commentsArea = commentsAreaRepository.findById(commentsId);
        Assert.isTrue(commentsArea.isPresent(), "评论不存在");
        return commentsArea.get();
    }


}
