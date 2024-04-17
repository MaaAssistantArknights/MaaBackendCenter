package plus.maa.backend.task

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import plus.maa.backend.service.level.ArkLevelService

@Component
class ArkLevelSyncTask(
    private val arkLevelService: ArkLevelService,
) {
    /**
     * 地图数据同步定时任务，每10分钟执行一次
     * 应用启动时自动同步一次
     */
    @Scheduled(cron = "\${maa-copilot.task-cron.ark-level:-}", zone = "Asia/Shanghai")
    suspend fun syncArkLevels() {
        arkLevelService.syncLevelData()
    }

    /**
     * 更新开放状态，每天凌晨执行，最好和热度值刷入任务保持相对顺序
     * 4:00、4:15 各执行一次，避免网络波动导致更新失败
     */
    @Scheduled(cron = "0 0-15/15 4 * * ?", zone = "Asia/Shanghai")
    suspend fun updateOpenStatus() = coroutineScope {
        awaitAll(
            async { arkLevelService.updateActivitiesOpenStatus() },
            async { arkLevelService.updateCrisisV2OpenStatus() },
        )
    }
}
