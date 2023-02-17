package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import plus.maa.backend.repository.entity.CommentsArea;

/**
 * @author LoMu
 * Date  2023-02-17 15:06
 */

@Repository
public interface CommentsAreaRepository extends MongoRepository<CommentsArea, String> {
    boolean existsCommentsAreasByCopilotId(Long copilotId);


}
