package plus.maa.backend.cache.transfer

import plus.maa.backend.repository.entity.Copilot
import java.util.concurrent.atomic.AtomicLong

data class CopilotInnerCacheInfo(
    val info: Copilot,
    val view: AtomicLong = AtomicLong(info.views),
)
