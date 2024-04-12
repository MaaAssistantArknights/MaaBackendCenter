package plus.maa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
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
}

fun UserRepository.findByUsersId(userId: List<String>): Map<String, MaaUser> = findAllById(userId).associateBy { it.userId!! }
