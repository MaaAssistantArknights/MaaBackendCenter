package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import plus.maa.backend.repository.entity.CommentsArea;

import java.util.Optional;

/**
 * @author LoMu
 * Date  2023-02-17 15:06
 */

@Repository
public interface CommentsAreaRepository extends MongoRepository<CommentsArea, String> {
    boolean existsCommentsAreasByCopilotId(Long copilotId);

    Optional<CommentsArea> findByCopilotId(Long copilotId);
}
