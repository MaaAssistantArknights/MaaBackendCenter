package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import plus.maa.backend.repository.entity.CopilotRating;

/**
 * @author LoMu
 * Date  2023-01-20 11:57
 */
@Repository
public interface CopilotRatingRepository extends MongoRepository<CopilotRating, String> {

    CopilotRating findByCopilotId(String copilotId);

    boolean existsCopilotRatingByCopilotId(String copilotId);
}
