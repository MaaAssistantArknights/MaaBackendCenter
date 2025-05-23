package plus.maa.backend.cache.transfer

import plus.maa.backend.controller.response.copilot.CopilotInfo
import java.util.concurrent.atomic.AtomicLong

data class CopilotInnerCacheInfo(
    val info: CopilotInfo,
    val view: AtomicLong = AtomicLong(info.views),
)
