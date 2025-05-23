package plus.maa.backend.cache

import com.github.benmanes.caffeine.cache.Caffeine
import plus.maa.backend.repository.entity.Copilot
import plus.maa.backend.repository.entity.MaaUser

class InternalComposeCache {
    // copilotId -> info
    val copilotCache = Caffeine.newBuilder()
        .softValues()
        .build<Long, Copilot?>()

    // copilotId -> info
    val maaUserCache = Caffeine.newBuilder()
        .softValues()
        .build<String, MaaUser>()

    // copilotId -> count
    val commentCountCache = Caffeine.newBuilder()
        .softValues()
        .build<Long, Long>()

    companion object {
        val Cache = InternalComposeCache()
    }

    fun invalidateCopilotInfoByCid(cid: Long?) {
        cid?.let {
            copilotCache.invalidate(it)
        }
    }

    fun invalidateMaaUserById(id: String?) {
        id?.let {
            maaUserCache.invalidate(it)
        }
    }

    fun invalidateCommentCountById(cid: Long?) {
        cid?.let {
            commentCountCache.invalidate(cid)
        }
    }
}
