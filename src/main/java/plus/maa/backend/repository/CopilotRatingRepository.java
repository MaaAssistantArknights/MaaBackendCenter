package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import plus.maa.backend.repository.entity.CopilotRating;

import java.util.Collection;
import java.util.List;

/**
 * @author LoMu
 * Date  2023-01-20 11:57
 */
public interface CopilotRatingRepository extends MongoRepository<CopilotRating, String> {

    CopilotRating findByCopilotId(Long copilotId);

    boolean existsCopilotRatingByCopilotId(Long copilotId);

    boolean existsCopilotRatingByCopilotIdAndDelete(Long copilotId, boolean delete);

    List<CopilotRating> findByCopilotIdIn(Collection<Long> copilotIds);

    List<CopilotRating> findByCopilotIdInAndDelete(Collection<Long> copilotIds, boolean delete);

}
