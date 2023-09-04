package plus.maa.backend.task;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import plus.maa.backend.BaseMockTest;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.Rating;
import plus.maa.backend.service.model.RatingCount;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

public class CopilotScoreRefreshTaskTest extends BaseMockTest {

    @InjectMocks
    CopilotScoreRefreshTask refreshTask;

    @Mock
    CopilotRepository copilotRepository;
    @Mock
    MongoTemplate mongoTemplate;
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

        // 配置copilotRepository
        when(copilotRepository.findAllByDeleteIsFalse(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(copilot1, copilot2, copilot3)));

        // 配置mongoTemplate
        when(mongoTemplate.aggregate(any(), eq(Rating.class), eq(RatingCount.class)))
                .thenReturn(new AggregationResults<>(List.of(
                        new RatingCount("1", 1L),
                        new RatingCount("2", 0L),
                        new RatingCount("3", 0L)), new Document()));

        refreshTask.refreshHotScores();

        assertTrue(copilot1.getHotScore() > 0);
        assertTrue(copilot2.getHotScore() > 0);
    }

    @Test
    void testRefreshTop100HotScores() {
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

        // 配置 RedisCache
        when(redisCache.getZSetReverse("rate:hot:copilotIds", 0, 99))
                .thenReturn(Set.of("1", "2", "3"));

        // 配置copilotRepository
        when(copilotRepository.findByCopilotIdInAndDeleteIsFalse(anyCollection()))
                .thenReturn(List.of(copilot1, copilot2, copilot3));

        // 配置mongoTemplate
        when(mongoTemplate.aggregate(any(), eq(Rating.class), eq(RatingCount.class)))
                .thenReturn(new AggregationResults<>(List.of(
                        new RatingCount("1", 1L),
                        new RatingCount("2", 0L),
                        new RatingCount("3", 0L)), new Document()));

        refreshTask.refreshTop100HotScores();

        assertTrue(copilot1.getHotScore() > 0);
        assertTrue(copilot2.getHotScore() > 0);
    }

}
