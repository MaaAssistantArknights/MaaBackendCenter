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
     * 地图数据同步定时任务，cron当前的意思为每小时执行一次
     */
    @Scheduled(cron = "0 0 */1 * * ?")
    public void syncArkLevels() {
        arkLevelService.runSyncLevelDataTask();
    }

}
