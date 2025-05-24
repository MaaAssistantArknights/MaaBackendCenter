package plus.maa.backend.cache

import com.github.benmanes.caffeine.cache.Caffeine
import plus.maa.backend.repository.entity.Copilot
import plus.maa.backend.repository.entity.MaaUser

object InternalComposeCache {
    // copilotId -> info
    private val copilotCache = Caffeine.newBuilder()
        .recordStats()
        .softValues()
        .build<Long, Copilot?>()

    // copilotId -> info
    private val maaUserCache = Caffeine.newBuilder()
        .recordStats()
        .softValues()
        .build<String, MaaUser>()

    // copilotId -> count
    val commentCountCache = Caffeine.newBuilder()
        .recordStats()
        .softValues()
        .build<Long, Long>()

    fun getCopilotCache(cid: Long, f: (Long) -> Copilot?): Copilot? {
        return copilotCache.get(cid, f)
    }

    fun getCopilotCache(cid: Long): Copilot? {
        return copilotCache.getIfPresent(cid)
    }

    fun getMaaUserCache(userId: String, f: (String) -> MaaUser): MaaUser {
        return maaUserCache.get(userId, f)
    }

    fun getMaaUserCache(userId: String): MaaUser? {
        return maaUserCache.getIfPresent(userId)
    }

    fun setMaaUserCache(userId: String, info: MaaUser) {
        maaUserCache.put(userId, info)
    }

    fun getCommentCountCache(cid: Long, f: (Long) -> Long): Long {
        return commentCountCache.get(cid, f)
    }

    fun getCommentCountCache(cid: Long): Long? {
        return commentCountCache.getIfPresent(cid)
    }

    fun setCommentCountCache(cid: Long, count: Long) {
        commentCountCache.put(cid, count)
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
