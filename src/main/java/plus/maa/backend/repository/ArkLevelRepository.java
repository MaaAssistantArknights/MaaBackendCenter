package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.ArkLevelSha;

import java.util.List;

/**
 * @author john180
 */
@Repository
public interface ArkLevelRepository extends MongoRepository<ArkLevel, String> {
    List<ArkLevelSha> findAllShaBy();

    @Query("""
            {
                "$or": [
                    {"levelId": ?0},
                    {"stageId": ?0},
                    {"catThree": ?0}
                ]
            }
            """)
    ArkLevel findByLevelId(String levelId);
}
