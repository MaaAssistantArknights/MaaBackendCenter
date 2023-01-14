package plus.maa.backend.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
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

    ArkLevel findByLevelId(String levelId);

    ArkLevel findByStageId(String stageId);
}
