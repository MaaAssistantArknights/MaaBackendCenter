package plus.maa.backend.repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import org.springframework.util.Assert;
import plus.maa.backend.repository.entity.CommentsArea;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;

/**
 * @author LoMu
 * Date 2023-01-20 14:59
 */
@Component
@RequiredArgsConstructor
public class TableLogicDelete {
    private final CopilotRatingRepository copilotRatingRepository;
    private final CopilotRepository copilotRepository;
    private final CommentsAreaRepository commentsAreaRepository;

    public void deleteCopilotById(String id) {
        Optional<Copilot> byId;

        if (StringUtils.isNumeric(id)) {
            byId = copilotRepository.findByCopilotId(Long.parseLong(id));
        } else {
            byId = copilotRepository.findById(id);
        }
        Assert.isTrue(byId.isPresent(), "作业表不存在");
        Assert.isTrue(!byId.get().isDelete(), "作业表不存在");

        Date date = new Date();
        Copilot copilot = byId.get();

        copilot.setDelete(true);
        copilot.setDeleteTime(date);
        copilotRepository.save(copilot);

        CopilotRating copilotRating = copilotRatingRepository.findByCopilotId(Long.valueOf(id));
        if (copilotRating != null) {
            copilotRating.setDelete(true);
            copilotRating.setDeleteTime(date);
            copilotRatingRepository.save(copilotRating);
        }
    }

    public void deleteCommentsId(String commentsId) {
        Date date = new Date();
        Optional<CommentsArea> byId = commentsAreaRepository.findById(commentsId);
        Assert.isTrue(byId.isPresent(), "评论表不存在");
        Assert.isTrue(!byId.get().isDelete(), "评论表不存在");

        CommentsArea commentsArea = byId.get();
        commentsArea.setDelete(true);
        commentsArea.setDeleteTime(date);

        //删除所有回复
        if (StringUtils.isBlank(commentsArea.getMainCommentId())) {
            Optional<List<CommentsArea>> optional = commentsAreaRepository.findByMainCommentId(commentsArea.getId());
            if (optional.isPresent()) {
                List<CommentsArea> commentsAreaList = optional.get();
                commentsAreaList.forEach(ca ->
                        ca.setDeleteTime(date)
                                .setDelete(true)
                );
                commentsAreaRepository.saveAll(commentsAreaList);
            }
        }
        commentsAreaRepository.save(commentsArea);
    }
}
