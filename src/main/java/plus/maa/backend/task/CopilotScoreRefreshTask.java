package plus.maa.backend.task;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import plus.maa.backend.repository.CopilotRatingRepository;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.repository.entity.CopilotRating;
import plus.maa.backend.service.CopilotService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作业热度值刷入任务，每日执行，用于计算基于时间的热度值
 *
 * @author dove
 * created on 2023.05.03
 */
@Component
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class CopilotScoreRefreshTask {

    RedisCache redisCache;
    CopilotRepository copilotRepository;
    CopilotRatingRepository copilotRatingRepository;

    /**
     * 热度值刷入任务，每日三点执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void refreshHotScores() {
        List<Copilot> copilots = copilotRepository.findAllByDeleteIsFalse();
        List<Copilot> changedCopilots = new ArrayList<>();
        List<Long> copilotIds = copilots.stream().map(Copilot::getCopilotId).toList();
        List<CopilotRating> ratings = copilotRatingRepository.findByCopilotIdIn(copilotIds);
        Map<Long, CopilotRating> ratingById = ratings.stream()
                .collect(Collectors.toMap(CopilotRating::getCopilotId, Function.identity(), (v1, v2) -> v1));
        for (Copilot copilot : copilots) {
            CopilotRating rating = ratingById.get(copilot.getCopilotId());
            copilot.setHotScore(CopilotService.getHotScore(copilot, rating));
            changedCopilots.add(copilot);
        }
        copilotRepository.saveAll(changedCopilots);
        // 移除首页热度缓存
        redisCache.removeCacheByPattern("home:hot:*");
    }

}
