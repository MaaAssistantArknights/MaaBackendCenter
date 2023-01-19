package plus.maa.backend.task;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import plus.maa.backend.service.ArkLevelService;

@Component
@RequiredArgsConstructor
public class ArkLevelSyncTask {

    private final ArkLevelService arkLevelService;

    /**
     * 地图数据同步定时任务，每10分钟执行一次
     * 应用启动时自动同步一次
     */
    @Scheduled(cron = "${maa-copilot.task-cron.ark-level:-}")
    public void syncArkLevels() {
        arkLevelService.runSyncLevelDataTask();
    }

}
