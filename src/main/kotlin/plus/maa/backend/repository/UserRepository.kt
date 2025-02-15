package plus.maa.backend.repository

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

    @Query("{ 'userName': { '\$regex': ?0, '\$options': 'i' } }")
    fun searchUsers(userName: String): List<MaaUserInfo>
}
