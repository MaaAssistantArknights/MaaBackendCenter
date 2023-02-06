package plus.maa.backend.repository;

import java.util.Date;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import plus.maa.backend.controller.response.MaaResultException;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;

/**
 * @author LoMu
 *         Date 2023-01-20 14:59
 */
@Component
@RequiredArgsConstructor
public class TableLogicDelete {
    private final CopilotRatingRepository copilotRatingRepository;
    private final CopilotRepository copilotRepository;

    public void deleteCopilotById(String id) {
        Optional<Copilot> byId;
        if (StringUtils.isNumeric(id)) {
            byId = copilotRepository.findByCopilotId(Long.parseLong(id));
        } else {
            byId = copilotRepository.findById(id);
        }
        if (byId.isEmpty())
            throw new MaaResultException("copilot Id不存在");
        Date date = new Date();
        Copilot copilot = byId.get();
        copilot.setDelete(true);
        copilot.setDeleteTime(date);
        copilotRepository.save(copilot);

        Optional<CopilotRating> copilotRating = copilotRatingRepository.findByCopilotId(Long.valueOf(id));
        copilotRating.ifPresent(rating -> {
            rating.setDelete(true);
            rating.setDeleteTime(date);
            copilotRatingRepository.save(rating);
        });

    }
}
