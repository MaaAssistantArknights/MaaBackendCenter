package plus.maa.backend.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import plus.maa.backend.repository.entity.CopilotRating;

/**
 * @author LoMu
 * Date  2023-01-20 11:57
 */
public interface CopilotRatingRepository extends MongoRepository<CopilotRating, String> {

    Optional<CopilotRating> findByCopilotId(Long copilotId);

    boolean existsCopilotRatingByCopilotId(String copilotId);
}
