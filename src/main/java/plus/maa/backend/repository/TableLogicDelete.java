package plus.maa.backend.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;

import java.util.Date;
import java.util.Optional;

/**
 * @author LoMu
 * Date  2023-01-20 14:59
 */
@Component
@RequiredArgsConstructor
public class TableLogicDelete {
    private final CopilotRatingRepository copilotRatingRepository;
    private final CopilotRepository copilotRepository;

    public void deleteCopilotById(String id) {
        Optional<Copilot> byId = copilotRepository.findById(id);
        if (byId.isEmpty()) throw new MaaResultException("copilot Id不存在");
        Date date = new Date();
        Copilot copilot = byId.get();
        copilot.setDelete(true);
        copilot.setDeleteTime(date);
        copilotRepository.save(copilot);

        CopilotRating copilotRating = copilotRatingRepository.findByCopilotId(id);
        if (ObjectUtils.isEmpty(copilotRating)) throw new RuntimeException("copilot rating is null");
        copilotRating.setDelete(true);
        copilotRating.setDeleteTime(date);

    }
}
