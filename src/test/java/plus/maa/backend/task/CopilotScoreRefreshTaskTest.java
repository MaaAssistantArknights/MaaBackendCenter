package plus.maa.backend.task;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import plus.maa.backend.BaseMockTest;
import plus.maa.backend.repository.CopilotRatingRepository;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.RatingRepository;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;
import plus.maa.backend.repository.entity.Rating;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class CopilotScoreRefreshTaskTest extends BaseMockTest {

    @InjectMocks
    CopilotScoreRefreshTask refreshTask;

    @Mock
    CopilotRepository copilotRepository;
    @Mock
    CopilotRatingRepository copilotRatingRepository;
    @Mock
    MongoTemplate mongoTemplate;
    @Mock
    RatingRepository ratingRepository;
    @Mock
    RedisCache redisCache;

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
        when(copilotRatingRepository.findByCopilotIdInAndDelete(eq(List.of(copilot1.getCopilotId(), copilot2.getCopilotId(),
                copilot3.getCopilotId())), eq(false)))
                .thenReturn(List.of(rating1, rating2));
        // 配置mongoTemplate
        when(mongoTemplate.aggregate(any(), eq(Rating.class), eq(CopilotScoreRefreshTask.RatingCount.class)))
                .thenReturn(new AggregationResults<>(List.of(
                        new CopilotScoreRefreshTask.RatingCount("1", 1L),
                        new CopilotScoreRefreshTask.RatingCount("2", 0L),
                        new CopilotScoreRefreshTask.RatingCount("3", 0L)), new Document()));
        // 配置 ratingRepository.insert 输入什么数组返回什么数组
        when(ratingRepository.insert(Collections.singleton(any()))).thenAnswer(invocation -> invocation.getArgument(0));
        // copilotRatingRepository.save
        when(copilotRatingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        refreshTask.refreshHotScores();

        assertTrue(copilot1.getHotScore() > 0);
        assertTrue(copilot2.getHotScore() > 0);
    }

}
