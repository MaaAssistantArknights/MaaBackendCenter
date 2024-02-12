package plus.maa.backend.repository

import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import plus.maa.backend.repository.entity.Rating

/**
 * @author lixuhuilll
 * Date  2023-08-20 12:06
 */
@Repository
interface RatingRepository : MongoRepository<Rating, String> {
    fun findByTypeAndKeyAndUserId(type: Rating.KeyType, key: String, userId: String): Rating?

}

