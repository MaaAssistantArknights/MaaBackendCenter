package plus.maa.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import plus.maa.backend.repository.entity.CopilotSet;

/**
 * @author dragove
 * create on 2024-01-01
 */
public interface CopilotSetRepository extends MongoRepository<CopilotSet, Long> {

    @Query("""
            {
                "$or": [
                    {"name": {'$regex': ?0 ,'$options':'i'}},
                    {"description": {'$regex': ?0,'$options':'i' }}
                ]
            }
            """)
    Page<CopilotSet> findByKeyword(String keyword, Pageable pageable);

}
