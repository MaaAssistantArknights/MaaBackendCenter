package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import plus.maa.backend.controller.request.CommentsRequest;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.repository.CommentsAreaRepository;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.entity.CommentsArea;
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

    /**
     * 评论
     * 每个评论都有一个uuid加持
     *
     * @param loginUser       登录用户
     * @param commentsRequest CommentsRequest
     * @return 评论 成功/失败
     */
    public MaaResult<String> addComments(LoginUser loginUser, CommentsRequest commentsRequest) {
        long copilotId = Long.parseLong(commentsRequest.getCopilotId());
        MaaUser maaUser = loginUser.getMaaUser();
        String message = commentsRequest.getMessage();
        CommentsArea commentsSection = new CommentsArea();
        //判断是否为子评论
        boolean notSubComment = StringUtils.isBlank(commentsRequest.getFromCommentsId()) && StringUtils.isBlank(commentsRequest.getFromSubCommentsId());

        Assert.isTrue(StringUtils.isNotBlank(message), "评论不可为空");
        Assert.isTrue(copilotRepository.existsCopilotsByCopilotId(copilotId), "作业表不存在");

        //评论表不存在 创建评论表
        if ((!commentsAreaRepository.existsCommentsAreasByCopilotId(copilotId)) && notSubComment) {
            commentsSection.setCopilotId(Long.parseLong(commentsRequest.getCopilotId()));

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
            CommentsArea one = mongoTemplate.findOne(query, CommentsArea.class);
            Assert.notNull(one, "评论表不存在");

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
            String commentsId = commentsRequest.getFromCommentsId();
            Query query = Query.query(Criteria.where("copilotId").is(copilotId));
            Update update = new Update();
            CommentsArea commentsArea = mongoTemplate.findOne(query, CommentsArea.class);
            Assert.notNull(commentsArea, "评论id不存在");

            Assert.isTrue(commentsArea.getCommentsInfos().stream()
                            .anyMatch(ci ->
                                    Objects.equals(commentsId, ci.getCommentsId())
                            )
                    , "回复的评论不存在");

            CommentsArea.SubCommentsInfo subCommentsInfo = new CommentsArea.SubCommentsInfo();

            //判断是回复给评论还是子评论
            if (StringUtils.isNotEmpty(commentsRequest.getFromSubCommentsId())) {
                String fromSubCommentsId = commentsRequest.getFromSubCommentsId();
                Assert.isTrue(commentsArea.getSubCommentsInfos().stream()
                                .anyMatch(sci ->
                                        Objects.equals(sci.getCommentsId(), fromSubCommentsId)
                                )
                        , "回复的评论不存在");
                subCommentsInfo
                        .setCommentsId(UUID.randomUUID().toString().replaceAll("-", ""))
                        .setFromCommentsId(commentsId)
                        .setFromSubCommentsId(commentsRequest.getFromSubCommentsId())
                        .setMessage(message)
                        .setUploaderId(maaUser.getUserId())
                        .setUploader(maaUser.getUserId());

            } else {
                subCommentsInfo
                        .setCommentsId(UUID.randomUUID().toString().replaceAll("-", ""))
                        .setFromCommentsId(commentsId)
                        .setMessage(message)
                        .setUploaderId(maaUser.getUserId())
                        .setUploader(maaUser.getUserId());
            }

            update.addToSet("subCommentsInfos", subCommentsInfo);
            mongoTemplate.updateFirst(query, update, CommentsArea.class);
        }

        return MaaResult.success("评论成功");
    }
}
