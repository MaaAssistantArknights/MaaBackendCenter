package plus.maa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import plus.maa.backend.repository.entity.UserFans

interface UserFansRepository : MongoRepository<UserFans, String> {
    fun findByUserId(userId: String): UserFans?
}
