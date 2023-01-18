package plus.maa.backend.task;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import plus.maa.backend.service.ArkLevelService;

@Component
@RequiredArgsConstructor
public class ArkLevelSyncTask {

    private final ArkLevelService arkLevelService;

    /**
     * 地图数据同步定时任务，cron当前的意思为每10分钟执行一次
     * 应用启动时自动同步一次
     */
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0/10 * * * ? ")
    public void syncArkLevels() {
        arkLevelService.runSyncLevelDataTask();
    }

}
