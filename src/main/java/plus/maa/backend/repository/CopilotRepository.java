package plus.maa.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import plus.maa.backend.repository.entity.CopilotOperation;

/**
 * @author LoMu
 * Date  2022-12-27 10:28
 */

@Repository
public interface CopilotRepository extends MongoRepository<CopilotOperation, String> {
    
}
