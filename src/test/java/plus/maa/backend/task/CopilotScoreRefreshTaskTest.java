package plus.maa.backend.task;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import plus.maa.backend.BaseMockTest;
import plus.maa.backend.repository.CopilotRatingRepository;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class CopilotScoreRefreshTaskTest extends BaseMockTest {

    @InjectMocks
    CopilotScoreRefreshTask refreshTask;

    @Mock
    CopilotRepository copilotRepository;
    @Mock
    CopilotRatingRepository copilotRatingRepository;

    @Test
    void testRefreshScores() {
        LocalDateTime now = LocalDateTime.now();
        Copilot copilot1 = new Copilot();
        copilot1.setCopilotId(1L);
        copilot1.setViews(100L);
        copilot1.setUploadTime(now);
        Copilot copilot2 = new Copilot();
        copilot2.setCopilotId(2L);
        copilot2.setViews(200L);
        copilot2.setUploadTime(now);
        Copilot copilot3 = new Copilot();
        copilot3.setCopilotId(3L);
        copilot3.setViews(200L);
        copilot3.setUploadTime(now);
        when(copilotRepository.findAllByDeleteIsFalse())
                .thenReturn(List.of(copilot1, copilot2, copilot3));
        CopilotRating rating1 = new CopilotRating(1L);
        rating1.setRatingUsers(List.of(new CopilotRating.RatingUser("a", "Like", now)));
        CopilotRating rating2 = new CopilotRating(2L);
        when(copilotRatingRepository.findByCopilotIdIn(eq(List.of(copilot1.getCopilotId(), copilot2.getCopilotId(),
                copilot3.getCopilotId()))))
                .thenReturn(List.of(rating1, rating2));
        refreshTask.refreshHotScores();

        assertTrue(copilot1.getHotScore() > 0);
        assertTrue(copilot2.getHotScore() > 0);
    }

}
