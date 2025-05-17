package plus.maa.backend.service

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import plus.maa.backend.controller.response.MaaResultException
import plus.maa.backend.controller.response.user.MaaUserInfo
import plus.maa.backend.repository.UserFansRepository
import plus.maa.backend.repository.UserFollowingRepository
import plus.maa.backend.repository.entity.MaaUser
import plus.maa.backend.repository.entity.UserFans
import plus.maa.backend.repository.entity.UserFollowing
import java.time.Instant

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

        // 检查是否已经关注
        val followQuery = Query.query(
            Criteria.where("userId").`is`(userId)
                .and("followList").`in`(followUserId),
        )
        if (mongoTemplate.exists(followQuery, UserFollowing::class.java)) {
            print("已关注 不可重复关注！")
            return
        }

        // 更新关注列表
        val followUpdate = Update()
            .addToSet("followList", followUserId)
            .set("updatedAt", Instant.now())

        mongoTemplate.upsert(
            Query.query(Criteria.where("userId").`is`(userId)),
            followUpdate,
            UserFollowing::class.java,
        )

        // 更新粉丝列表
        val fansUpdate = Update()
            .addToSet("fansList", userId)
            .set("updatedAt", Instant.now())

        mongoTemplate.upsert(
            Query.query(Criteria.where("userId").`is`(followUserId)),
            fansUpdate,
            UserFans::class.java,
        )

        // 更新关注数量和粉丝数量

        val followingCount = userFollowingRepository.findByUserId(userId)?.followList?.size ?: 0
        val fansCount = userFansRepository.findByUserId(followUserId)?.fansList?.size ?: 0

        mongoTemplate.updateFirst(
            Query.query(Criteria.where("userId").`is`(userId)),
            Update().set("followingCount", followingCount),
            MaaUser::class.java,
        )

        mongoTemplate.updateFirst(
            Query.query(Criteria.where("userId").`is`(followUserId)),
            Update().set("fansCount", fansCount),
            MaaUser::class.java,
        )
    }

    @Transactional
    fun unfollow(userId: String, followUserId: String) {
        require(userId != followUserId) { "不能取关自己" }

        // 检查是否已经关注
        val followQuery = Query.query(
            Criteria.where("userId").`is`(userId)
                .and("followList").`in`(followUserId),
        )
        if (!mongoTemplate.exists(followQuery, UserFollowing::class.java)) {
            return
        }

        // 更新关注列表
        val followUpdate = Update()
            .pull("followList", followUserId)
            .set("updatedAt", Instant.now())

        mongoTemplate.updateFirst(
            Query.query(Criteria.where("userId").`is`(userId)),
            followUpdate,
            UserFollowing::class.java,
        )

        // 更新粉丝列表
        val fansUpdate = Update()
            .pull("fansList", userId)
            .set("updatedAt", Instant.now())

        mongoTemplate.updateFirst(
            Query.query(Criteria.where("userId").`is`(followUserId)),
            fansUpdate,
            UserFans::class.java,
        )

        // 更新关注数量和粉丝数量

        val followingCount = userFollowingRepository.findByUserId(userId)?.followList?.size ?: 0
        val fansCount = userFansRepository.findByUserId(followUserId)?.fansList?.size ?: 0

        mongoTemplate.updateFirst(
            Query.query(Criteria.where("userId").`is`(userId)),
            Update().set("followingCount", followingCount),
            MaaUser::class.java,
        )

        mongoTemplate.updateFirst(
            Query.query(Criteria.where("userId").`is`(followUserId)),
            Update().set("fansCount", fansCount),
            MaaUser::class.java,
        )
    }

    fun getFollowingList(userId: String, pageable: Pageable): Page<MaaUserInfo> {
        val following = userFollowingRepository.findByUserId(userId)
            ?: return Page.empty(pageable)

        val followIds = following.followList
        val total = followIds.size.toLong()
        val start = pageable.offset.coerceAtMost(total)
        val end = (start + pageable.pageSize).coerceAtMost(total)

        if (start >= total) {
            return Page.empty(pageable)
        }

        val pageIds = followIds.subList(start.toInt(), end.toInt())
        val users = mongoTemplate.find(
            Query.query(Criteria.where("userId").`in`(pageIds)), // 注意这里用 userId 字段查询
            MaaUser::class.java,
        )

        val userMap = users.associateBy { it.userId }
        val userInfos = pageIds.mapNotNull { id ->
            userMap[id]?.let { MaaUserInfo(it) }
        }

        return PageImpl(userInfos, pageable, total)
    }

    fun getFansList(userId: String, pageable: Pageable): Page<MaaUserInfo> {
        val fans = userFansRepository.findByUserId(userId)
            ?: return Page.empty(pageable)

        val fanIds = fans.fansList
        val total = fanIds.size.toLong()
        val start = pageable.offset.coerceAtMost(total)
        val end = (start + pageable.pageSize).coerceAtMost(total)

        if (start >= total) {
            return Page.empty(pageable)
        }

        val pageIds = fanIds.subList(start.toInt(), end.toInt())
        val users = mongoTemplate.find(
            Query.query(Criteria.where("userId").`in`(pageIds)),
            MaaUser::class.java,
        )

        val userMap = users.associateBy { it.userId }
        val userInfos = pageIds.mapNotNull { id ->
            userMap[id]?.let { MaaUserInfo(it) }
        }

        return PageImpl(userInfos, pageable, total)
    }
}
