package plus.maa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import plus.maa.backend.repository.entity.UserFollowing

interface UserFollowingRepository : MongoRepository<UserFollowing, String> {
    fun findByUserId(userId: String): UserFollowing?
}
