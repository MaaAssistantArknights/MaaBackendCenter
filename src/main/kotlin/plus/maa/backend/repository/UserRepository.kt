package plus.maa.backend.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import plus.maa.backend.controller.response.user.MaaUserInfo
import plus.maa.backend.repository.entity.MaaUser

/**
 * @author AnselYuki
 */
interface UserRepository : MongoRepository<MaaUser, String> {
    /**
     * 根据邮箱（用户唯一登录凭据）查询
     *
     * @param email 邮箱字段
     * @return 查询用户
     */
    fun findByEmail(email: String): MaaUser?

    fun findByUserId(userId: String): MaaUser?

    @Query("{ 'userName': { '\$regex': ?0, '\$options': 'i' }, 'status': 1 }")
    fun searchUsers(userName: String, pageable: Pageable): Page<MaaUserInfo>

    /**
     * 查找多个用户ID对应的用户信息
     * 用于批量获取关注或粉丝列表中的用户
     */
    @Query("{ 'userId': { '\$in': ?0 } }")
    fun findByUserIdIn(userIds: Collection<String>, pageable: Pageable): Page<MaaUser>
}
