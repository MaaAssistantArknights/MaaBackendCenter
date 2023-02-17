package plus.maa.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import plus.maa.backend.controller.request.CommentsRequest;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.repository.CommentsSectionRepository;
import plus.maa.backend.repository.entity.CommentsSection;
import plus.maa.backend.service.model.LoginUser;

import java.util.List;

/**
 * @author LoMu
 * Date  2023-02-17 15:00
 */

@Service
@RequiredArgsConstructor
public class CommentsSectionService {
    private final CommentsSectionRepository commentsSectionRepository;

    public MaaResult<String> sendComments(LoginUser loginUser, CommentsRequest commentsRequest) {
        String content = commentsRequest.getContent();
        CommentsSection commentsSection = new CommentsSection();
        commentsSection.setCopilotId(commentsRequest.getCopilotId());
        commentsSection.setCommentsUserInfos(
                List.of(
                        new CommentsSection.CommentsUserInfo()
                                .setUploaderId(loginUser.getMaaUser().getUserId())
                                .setUploader(loginUser.getMaaUser().getUserName())
                                .setContent(content)
                ));
        commentsSectionRepository.insert(commentsSection);
        return MaaResult.success("评论成功");
    }
}
