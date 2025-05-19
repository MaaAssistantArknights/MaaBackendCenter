package plus.maa.backend.controller.response.copilot

import java.util.concurrent.atomic.AtomicLong

data class CopilotInnerCacheInfo(
    val info: CopilotInfo,
    val view: AtomicLong = AtomicLong(info.views),
)
