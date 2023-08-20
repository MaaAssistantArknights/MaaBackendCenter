package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import plus.maa.backend.repository.entity.Rating;
import plus.maa.backend.service.model.RatingType;

import java.util.Optional;

/**
 * @author lixuhuilll
 * Date  2023-08-20 12:06
 */

@Repository
public interface RatingRepository extends MongoRepository<Rating, String> {
    Optional<Rating> findByTypeAndKeyAndUserId(Rating.KeyType type, String key, String userId);

    long countByTypeAndKeyAndRating(Rating.KeyType type, String key, RatingType rating);

    long countByTypeAndKey(Rating.KeyType type, String key);
}

