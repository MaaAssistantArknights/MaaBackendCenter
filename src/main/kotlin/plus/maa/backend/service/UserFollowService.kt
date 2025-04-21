package plus.maa.backend.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import plus.maa.backend.controller.response.MaaResultException
import plus.maa.backend.controller.response.user.MaaUserInfo
import plus.maa.backend.repository.UserFansRepository
import plus.maa.backend.repository.UserFollowingRepository
import plus.maa.backend.repository.entity.MaaUser
import plus.maa.backend.repository.entity.UserFans
import plus.maa.backend.repository.entity.UserFollowing
import java.time.LocalDateTime

@Service
class UserFollowService(
    private val userFollowingRepository: UserFollowingRepository,
    private val userFansRepository: UserFansRepository,
    private val userService: UserService,
    private val mongoTemplate: MongoTemplate,
) {
    @Transactional
    fun follow(userId: String, followUserId: String) {
        require(userId != followUserId) { "不能关注自己" }

        // 检查被关注用户是否存在
        val targetUser = userService.findByUserIdOrDefault(followUserId)
        if (targetUser == MaaUser.UNKNOWN) {
            throw MaaResultException(404, "目标用户不存在")
        }

        // 获取当前用户信息
        val currentUser = userService.findByUserIdOrDefault(userId)

        // 将 MaaUser 转换为 MaaUserInfo
        val targetUserInfo = MaaUserInfo(
            id = targetUser.userId!!,
            userName = targetUser.userName,
            activated = targetUser.status == 1,
            followingCount = targetUser.followingCount,
            fansCount = targetUser.fansCount,
        )
        val currentUserInfo = MaaUserInfo(
            id = currentUser.userId!!,
            userName = currentUser.userName,
            activated = currentUser.status == 1,
            followingCount = currentUser.followingCount,
            fansCount = currentUser.fansCount,
        )

        // 更新关注列表
        val following = userFollowingRepository.findByUserId(userId)
            ?: UserFollowing(userId = userId)
        if (!following.followList.any { it.id == followUserId }) {
            following.followList.add(targetUserInfo)
            following.updatedAt = LocalDateTime.now()
            userFollowingRepository.save(following)

            // 更新当前用户的关注数（基于集合大小）
            val currentUserQuery = Query.query(Criteria.where("userId").`is`(userId))
            val currentUserUpdate = org.springframework.data.mongodb.core.query.Update().set("followingCount", following.followList.size)
            mongoTemplate.updateFirst(currentUserQuery, currentUserUpdate, MaaUser::class.java)
        }

        // 更新粉丝列表
        val fans = userFansRepository.findByUserId(followUserId)
            ?: UserFans(userId = followUserId)
        if (!fans.fansList.any { it.id == userId }) {
            fans.fansList.add(currentUserInfo)
            fans.updatedAt = LocalDateTime.now()
            userFansRepository.save(fans)

            // 更新目标用户的粉丝数（基于集合大小）
            val targetUserQuery = Query.query(Criteria.where("userId").`is`(followUserId))
            val targetUserUpdate = org.springframework.data.mongodb.core.query.Update().set("fansCount", fans.fansList.size)
            mongoTemplate.updateFirst(targetUserQuery, targetUserUpdate, MaaUser::class.java)
        }
    }

    @Transactional
    fun unfollow(userId: String, followUserId: String) {
        require(userId != followUserId) { "不能取关自己" }

        // 更新关注列表
        userFollowingRepository.findByUserId(userId)?.let { following ->
            if (following.followList.removeIf { it.id == followUserId }) {
                following.updatedAt = LocalDateTime.now()
                userFollowingRepository.save(following)

                // 更新当前用户的关注数（基于集合大小）
                val currentUserQuery = Query.query(Criteria.where("userId").`is`(userId))
                val currentUserUpdate = org.springframework.data.mongodb.core.query.Update().set(
                    "followingCount",
                    following.followList.size,
                )
                mongoTemplate.updateFirst(currentUserQuery, currentUserUpdate, MaaUser::class.java)
            }
        }

        // 更新粉丝列表
        userFansRepository.findByUserId(followUserId)?.let { fans ->
            if (fans.fansList.removeIf { it.id == userId }) {
                fans.updatedAt = LocalDateTime.now()
                userFansRepository.save(fans)

                // 更新目标用户的粉丝数（基于集合大小）
                val targetUserQuery = Query.query(Criteria.where("userId").`is`(followUserId))
                val targetUserUpdate = org.springframework.data.mongodb.core.query.Update().set("fansCount", fans.fansList.size)
                mongoTemplate.updateFirst(targetUserQuery, targetUserUpdate, MaaUser::class.java)
            }
        }
    }

    fun getFollowingList(userId: String, pageable: Pageable): Page<MaaUserInfo> {
        val following = userFollowingRepository.findByUserId(userId)
            ?: return Page.empty(pageable)

        val totalElements = following.followList.size.toLong()
        val start = pageable.pageNumber * pageable.pageSize
        val end = minOf(start + pageable.pageSize, totalElements.toInt())

        if (start >= totalElements) {
            return Page.empty(pageable)
        }

        val content = following.followList.toMutableList()
            .subList(start, end)

        return PageImpl(content, pageable, totalElements)
    }

    fun getFansList(userId: String, pageable: Pageable): Page<MaaUserInfo> {
        val fans = userFansRepository.findByUserId(userId)
            ?: return Page.empty(pageable)

        val totalElements = fans.fansList.size.toLong()
        val start = pageable.pageNumber * pageable.pageSize
        val end = minOf(start + pageable.pageSize, totalElements.toInt())

        if (start >= totalElements) {
            return Page.empty(pageable)
        }

        val content = fans.fansList.toMutableList()
            .subList(start, end)

        return PageImpl(content, pageable, totalElements)
    }
}
