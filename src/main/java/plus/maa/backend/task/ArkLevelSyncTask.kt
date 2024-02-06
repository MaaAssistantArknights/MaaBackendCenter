package plus.maa.backend.task;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
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

    /**
     * 更新开放状态，每天凌晨执行，最好和热度值刷入任务保持相对顺序
     * 4:00、4:15 各执行一次，避免网络波动导致更新失败
     */
    @Scheduled(cron = "0 0-15/15 4 * * ?")
    public void updateOpenStatus() {
        arkLevelService.updateActivitiesOpenStatus();
        arkLevelService.updateCrisisV2OpenStatus();
    }

}
