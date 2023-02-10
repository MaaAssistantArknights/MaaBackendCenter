package plus.maa.backend.repository;

import java.util.Collection;
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
    Stream<ArkLevel> findByLevelIdFuzzy(String levelId);

    /**
     * 用于前端查询 关卡名、关卡类型、关卡编号
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
    Stream<ArkLevel> queryLevelByKeyword(String keyword);

    /**
     * 根据stageId列表查询
     */
    List<ArkLevel> findByStageIdIn(Collection<String> stageIds);

}
