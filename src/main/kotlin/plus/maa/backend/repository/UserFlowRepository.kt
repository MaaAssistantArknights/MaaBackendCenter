package plus.maa.backend.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import plus.maa.backend.repository.entity.UserFlow

interface UserFlowRepository : MongoRepository<UserFlow, String> {

    /**
     * 查询关注关系
     */
    fun findByUserIdAndFollowUserIdAndStatus(userId: String, followUserId: String, status: Int): UserFlow?

    /**
     * 获取用户的关注列表
     */
    fun findByUserIdAndStatus(userId: String, status: Int, pageable: Pageable): Page<UserFlow>

    /**
     * 获取用户的粉丝列表
     */
    fun findByFollowUserIdAndStatus(followUserId: String, status: Int, pageable: Pageable): Page<UserFlow>

    /**
     * 获取用户的关注数量
     */
    fun countByUserIdAndStatus(userId: String, status: Int): Long

    /**
     * 获取用户的粉丝数量
     */
    fun countByFollowUserIdAndStatus(followUserId: String, status: Int): Long

    /**
     * 直接查询关注的用户ID集合
     */
    @Query(value = "{'userId': ?0, 'status': ?1}", fields = "{'followUserId': 1}")
    fun findFollowingIds(userId: String, status: Int): Set<String>

    /**
     * 直接查询粉丝ID集合
     */
    @Query(value = "{'followUserId': ?0, 'status': ?1}", fields = "{'userId': 1}")
    fun findFollowerIds(followUserId: String, status: Int): Set<String>
}
