package plus.maa.backend.repository;

import java.util.List;
import java.util.stream.Stream;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.ArkLevelSha;

/**
 * @author john180
 */
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
    Stream<ArkLevel> findByLevelId(String levelId);

    /**
     * 用于前端查询 关卡名、关卡类型、关卡编号
     *
     * @param stageId stageId
     * @return ArkLevel
     */
    @Query("""
            {
                "$or": [
                    {"stageId": {$regex: ?0 }},
                    {"catThree": {$regex: ?0 }},
                    {"catTwo":  {$regex: ?0 }},
                    {"catOne": {$regex: ?0 }},
                    {"name": {$regex: ?0 }}
                ]
            }
            """)
    Stream<ArkLevel> queryLevel(String stageId);
}
