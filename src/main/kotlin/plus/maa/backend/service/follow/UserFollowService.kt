package plus.maa.backend.service.follow

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.ArrayOperators
import org.springframework.data.mongodb.core.aggregation.ConvertOperators
import org.springframework.data.mongodb.core.aggregation.VariableOperators
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import plus.maa.backend.controller.response.user.MaaUserInfo
import plus.maa.backend.repository.entity.MaaUser
import plus.maa.backend.repository.entity.UserFans
import plus.maa.backend.repository.entity.UserFollowing
import java.time.Instant
import kotlin.reflect.KClass

@Service
class UserFollowService(private val mongoTemplate: MongoTemplate) {

    @Transactional
    fun follow(userId: String, followUserId: String) = updateFollowingRel(userId, followUserId, true)

    @Transactional
    fun unfollow(userId: String, followUserId: String) = updateFollowingRel(userId, followUserId, false)

    private fun updateFollowingRel(followerId: String, followeeId: String, add: Boolean) {
        val opStr = if (add) "关注" else "取关"
        require(followerId != followeeId) { "不能${opStr}自己" }
        if (!mongoTemplate.exists(Query.query(Criteria.where("userId").`is`(followeeId)), MaaUser::class.java)) {
            throw IllegalArgumentException("${opStr}对象不存在")
        }

        updateUserListAndCount(followerId, "followingCount", UserFollowing::class, "followList", add, followeeId)
        updateUserListAndCount(followeeId, "fansCount", UserFans::class, "fansList", add, followerId)
    }

    private fun <T : Any> updateUserListAndCount(
        ownerId: String,
        ownerCountField: String,
        srcClazz: KClass<T>,
        srcListField: String,
        add: Boolean,
        userId: String,
    ) {
        val userIdMatch = Criteria.where("userId").`is`(ownerId)
        val update = Update().apply {
            (if (add) ::addToSet else ::pull).invoke(srcListField, userId)
            set("updatedAt", Instant.now())
        }
        mongoTemplate.upsert(Query.query(userIdMatch), update, srcClazz.java)

        val cR = mongoTemplate.aggregate(
            Aggregation.newAggregation(
                Aggregation.match(userIdMatch),
                Aggregation.project().and(ArrayOperators.arrayOf(srcListField).length()).`as`("total"),
            ),
            srcClazz.java,
            CountResult::class.java,
        ).uniqueMappedResult ?: return
        mongoTemplate.updateFirst(
            Query.query(userIdMatch),
            Update().set(ownerCountField, cR.total),
            MaaUser::class.java,
        )
    }

    fun getFollowingList(userId: String, pageable: Pageable) = getReferredUserPage(userId, UserFollowing::class, "followList", pageable)

    fun getFansList(userId: String, pageable: Pageable) = getReferredUserPage(userId, UserFans::class, "fansList", pageable)

    private fun <T : Any> getReferredUserPage(ownerId: String, clazz: KClass<T>, field: String, pageable: Pageable): PageImpl<MaaUserInfo> {
        val match = Aggregation.match(Criteria.where("userId").`is`(ownerId))

        val slice = ArrayOperators.arrayOf(field).slice().offset(pageable.pageNumber * pageable.pageSize).itemCount(pageable.pageSize)
        val slicedIds = VariableOperators.mapItemsOf(slice).`as`("id").andApply(ConvertOperators.valueOf("id").convertToObjectId())
        val extractCountAndIds = Aggregation.project().and(ArrayOperators.arrayOf(field).length()).`as`("total").and(slicedIds).`as`("ids")

        val lookupUsers = Aggregation.lookup("maa_user", "ids", "_id", "paged")

        val result = mongoTemplate.aggregate(
            Aggregation.newAggregation(match, extractCountAndIds, lookupUsers),
            clazz.java,
            PagedUserListResult::class.java,
        ).uniqueMappedResult

        val userInfos = result?.paged.orEmpty().map(::MaaUserInfo)
        return PageImpl(userInfos, pageable, result?.total ?: 0L)
    }
}
