package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import plus.maa.backend.controller.request.CommentsAddDto;
import plus.maa.backend.controller.request.CommentsRatingDTO;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.controller.response.MaaResultException;
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
     * @param commentsAddDto CommentsRequest
     * @return 评论 成功/失败
     */
    public MaaResult<String> addComments(LoginUser loginUser, CommentsAddDto commentsAddDto) {
        long copilotId = Long.parseLong(commentsAddDto.getCopilotId());
        MaaUser maaUser = loginUser.getMaaUser();
        String message = commentsAddDto.getMessage();
        CommentsArea commentsSection = new CommentsArea();
        //判断是否为子评论
        boolean notSubComment = StringUtils.isBlank(commentsAddDto.getFromCommentsId()) && StringUtils.isBlank(commentsAddDto.getFromSubCommentsId());

        Assert.isTrue(StringUtils.isNotBlank(message), "评论不可为空");
        Assert.isTrue(copilotRepository.existsCopilotsByCopilotId(copilotId), "作业表不存在");

        //评论表不存在 创建评论表
        if ((!commentsAreaRepository.existsCommentsAreasByCopilotId(copilotId)) && notSubComment) {
            commentsSection.setCopilotId(copilotId);

            CommentsArea.CommentsInfo commentsInfo = new CommentsArea.CommentsInfo()
                    .setCommentsId(UUID.randomUUID().toString().replaceAll("-", ""))
                    .setUploaderId(loginUser.getMaaUser().getUserId())
                    .setUploader(loginUser.getMaaUser().getUserName())
                    .setMessage(message);

            ArrayList<CommentsArea.CommentsInfo> commentsInfoArrayList = new ArrayList<>();
            commentsInfoArrayList.add(commentsInfo);
            commentsSection.setCommentsInfos(commentsInfoArrayList);
            commentsAreaRepository.insert(commentsSection);
        }

        //评论表存在、进行回复
        else if (notSubComment) {
            Query query = Query.query(Criteria.where("copilotId").is(copilotId));
            Update update = new Update();

            CommentsArea.CommentsInfo commentsInfo = new CommentsArea.CommentsInfo();
            commentsInfo
                    .setCommentsId(UUID.randomUUID().toString().replaceAll("-", ""))
                    .setUploaderId(maaUser.getUserId())
                    .setUploader(maaUser.getUserName())
                    .setMessage(message);
            update.addToSet("commentsInfos", commentsInfo);

            mongoTemplate.updateFirst(query, update, CommentsArea.class);
        }

        //回复子评论
        else {
            String commentsId = commentsAddDto.getFromCommentsId();
            Query query = Query.query(Criteria.where("copilotId").is(copilotId));
            Update update = new Update();
            CommentsArea commentsArea = findCommentsAreaByCopilotId(copilotId);

            Assert.isTrue(commentsArea.getCommentsInfos().stream()
                            .anyMatch(ci ->
                                    Objects.equals(commentsId, ci.getCommentsId())
                            )
                    , "回复的评论不存在");

            CommentsArea.CommentsInfo commentsInfo = new CommentsArea.CommentsInfo();

            //判断是回复给评论还是子评论
            if (StringUtils.isNotEmpty(commentsAddDto.getFromSubCommentsId())) {
                String fromSubCommentsId = commentsAddDto.getFromSubCommentsId();
                Assert.isTrue(commentsArea.getCommentsInfos().stream()
                                .anyMatch(sci ->
                                        Objects.equals(sci.getCommentsId(), fromSubCommentsId)
                                )
                        , "回复的评论不存在");
                commentsInfo
                        .setFromCommentsId(commentsId)
                        .setFromSubCommentsId(commentsAddDto.getFromSubCommentsId())
                        .setMessage(message)
                        .setUploaderId(maaUser.getUserId())
                        .setUploader(maaUser.getUserId())
                        .setCommentsId(UUID.randomUUID().toString().replaceAll("-", ""));


            } else {
                commentsInfo
                        .setFromCommentsId(commentsId)
                        .setMessage(message)
                        .setUploaderId(maaUser.getUserId())
                        .setUploader(maaUser.getUserId())
                        .setCommentsId(UUID.randomUUID().toString().replaceAll("-", ""));
            }

            update.addToSet("commentsInfos", commentsInfo);
            mongoTemplate.updateFirst(query, update, CommentsArea.class);
        }

        return MaaResult.success("评论成功");
    }


    public MaaResult<String> deleteComments(LoginUser loginUser, String copilotId, String commentsId) {
        long id = Long.parseLong(copilotId);
        CommentsArea.CommentsInfo comments = findCommentsId(id, commentsId);
        verifyOwner(loginUser, comments.getUploaderId());

        tableLogicDelete.deleteCommentsId(id, commentsId);
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
        long id = Long.parseLong(commentsRatingDTO.getCopilotId());
        boolean existRatingUser = false;

        CommentsArea commentsArea = findCommentsAreaByCopilotId(id);
        List<CommentsArea.CommentsInfo> commentsInfos = commentsArea.getCommentsInfos();


        for (CommentsArea.CommentsInfo info : commentsInfos) {
            if (Objects.equals(info.getCommentsId(), commentsRatingDTO.getCommentsId())) {
                List<CopilotRating.RatingUser> ratingUserList = info.getRatingUser();
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
                info.setRatingUser(ratingUserList);
            }
        }

        commentsAreaRepository.save(commentsArea);
        return MaaResult.success("成功");
    }


    /**
     * 查询指定作业评论
     *
     * @param id         作业id
     * @param commentsId 评论id
     * @return CommentsInfo
     */
    private CommentsArea.CommentsInfo findCommentsId(Long id, String commentsId) {
        CommentsArea commentsArea = findCommentsAreaByCopilotId(id);
        Optional<CommentsArea.CommentsInfo> commentsInfoOptional = commentsArea.getCommentsInfos().stream()
                .filter(ci ->
                        Objects.equals(ci.getCommentsId(), commentsId)
                ).findFirst();

        if (commentsInfoOptional.isPresent()) {
            return commentsInfoOptional.get();
        } else {
            throw new MaaResultException("评论不存在");
        }
    }


    private void verifyOwner(LoginUser user, String uploaderId) {
        Assert.state(Objects.equals(user.getMaaUser().getUserId(), uploaderId), "您无法删除不属于您的评论");
    }

    /**
     * 获取对应作业的评论表
     *
     * @param copilotId 作业id
     * @return CommentsArea
     */
    private CommentsArea findCommentsAreaByCopilotId(Long copilotId) {
        Optional<CommentsArea> commentsAreaOptional = commentsAreaRepository.findByCopilotId(copilotId);
        Assert.isTrue(commentsAreaOptional.isPresent(), "评论表不存在");
        return commentsAreaOptional.get();
    }
}
