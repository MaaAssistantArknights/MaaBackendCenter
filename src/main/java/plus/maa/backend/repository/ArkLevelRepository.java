package plus.maa.backend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.ArkLevelSha;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * @author john180
 */
public interface ArkLevelRepository extends MongoRepository<ArkLevel, String> {
    List<ArkLevelSha> findAllShaBy();

    Page<ArkLevel> findAllByCatOne(String catOne, Pageable pageable);

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
                    {"stageId": {'$regex': ?0 ,'$options':'i'}},
                    {"catThree": {'$regex': ?0 ,'$options':'i'}},
                    {"catTwo":  {'$regex': ?0 ,'$options':'i'}},
                    {"catOne": {'$regex': ?0 ,'$options':'i'}},
                    {"name": {'$regex': ?0,'$options':'i' }}
                ]
            }
            """)
    Stream<ArkLevel> queryLevelByKeyword(String keyword);

    /**
     * 根据stageId列表查询
     */
    List<ArkLevel> findByStageIdIn(Collection<String> stageIds);

}
