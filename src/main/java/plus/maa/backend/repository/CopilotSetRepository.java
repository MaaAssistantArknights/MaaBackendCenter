package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import plus.maa.backend.repository.entity.CopilotSet;

/**
 * @author dragove
 * create on 2024-01-01
 */
public interface CopilotSetRepository extends MongoRepository<CopilotSet, Long> {


}
