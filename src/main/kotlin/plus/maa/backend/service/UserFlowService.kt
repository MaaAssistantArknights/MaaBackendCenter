package plus.maa.backend.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import plus.maa.backend.controller.response.MaaResultException
import plus.maa.backend.controller.response.user.MaaUserInfo
import plus.maa.backend.repository.UserFlowRepository
import plus.maa.backend.repository.entity.MaaUser
import plus.maa.backend.repository.entity.UserFlow
import java.time.LocalDateTime

@Service
class UserFlowService(
    private val userFlowRepository: UserFlowRepository,
    private val userService: UserService,
) {
    /**
     * 关注用户
     */
    fun follow(userId: String, followUserId: String) {
        require(userId != followUserId) { "不能关注自己" }

        // 检查被关注用户是否存在
        val userOrDefault = userService.findByUserIdOrDefault(followUserId)
        if (userOrDefault == MaaUser.UNKNOWN) {
            throw MaaResultException(404, "目标用户不存在")
        }

        // 检查是否已关注
        val existFlow = userFlowRepository.findByUserIdAndFollowUserIdAndStatus(userId, followUserId, 1)
        if (existFlow != null) {
            return
        }

        val userFlow = UserFlow(
            userId = userId,
            followUserId = followUserId,
        )
        userFlowRepository.save(userFlow)
    }

    /**
     * 取消关注
     */
    fun unfollow(userId: String, followUserId: String) {
        val userFlow = userFlowRepository.findByUserIdAndFollowUserIdAndStatus(userId, followUserId, 1)
            ?: return

        userFlow.status = 0
        userFlow.updateTime = LocalDateTime.now()
        userFlowRepository.save(userFlow)
    }

    /**
     * 获取关注列表
     */
    fun getFollowingList(userId: String, pageable: Pageable): Page<MaaUserInfo> {
        val flowPage = userFlowRepository.findByUserIdAndStatus(userId, 1, pageable)
        return flowPage.map { flow ->
            val targetUser = userService.findByUserIdOrDefault(flow.followUserId)
            MaaUserInfo(targetUser)
        }
    }

    /**
     * 获取粉丝列表
     */
    fun getFollowerList(userId: String, pageable: Pageable): Page<MaaUserInfo> {
        val flowPage = userFlowRepository.findByFollowUserIdAndStatus(userId, 1, pageable)
        return flowPage.map { flow ->
            val follower = userService.findByUserIdOrDefault(flow.userId)
            MaaUserInfo(follower)
        }
    }

    /**
     * 获取关注数量
     */
    fun getFollowingCount(userId: String): Long {
        return userFlowRepository.countByUserIdAndStatus(userId, 1)
    }

    /**
     * 获取粉丝数量
     */
    fun getFollowerCount(userId: String): Long {
        return userFlowRepository.countByFollowUserIdAndStatus(userId, 1)
    }

    /**
     * 检查是否已关注
     */
    fun isFollowing(userId: String, followUserId: String): Boolean {
        return userFlowRepository.findByUserIdAndFollowUserIdAndStatus(userId, followUserId, 1) != null
    }

    /**
     * 获取所有关注的用户ID集合
     */
    fun getAllFollowingIdSet(userId: String): Set<String> {
        return userFlowRepository.findFollowingIds(userId, 1)
    }

    /**
     * 获取所有粉丝的用户ID集合
     */
    fun getAllFollowerIdSet(userId: String): Set<String> {
        return userFlowRepository.findFollowerIds(userId, 1)
    }
}
