package plus.maa.backend.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.Query
import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.ArkLevelSha

/**
 * @author john180
 */
interface ArkLevelRepository : MongoRepository<ArkLevel, String> {
    fun findAllShaBy(): List<ArkLevelSha>

    fun findAllByCatOne(catOne: String, pageable: Pageable): Page<ArkLevel>

    @Query(
        """
            {
                "${'$'}or": [
                    {"levelId": ?0},
                    {"stageId": ?0},
                    {"catThree": ?0}
                ]
            }
            
            """
    )
    fun findByLevelIdFuzzy(levelId: String): List<ArkLevel>

    /**
     * 用于前端查询 关卡名、关卡类型、关卡编号
     */
    @Query(
        """
            {
                "${'$'}or": [
                    {"stageId": {'${'$'}regex': ?0 ,'${'$'}options':'i'}},
                    {"catThree": {'${'$'}regex': ?0 ,'${'$'}options':'i'}},
                    {"catTwo":  {'${'$'}regex': ?0 ,'${'$'}options':'i'}},
                    {"catOne": {'${'$'}regex': ?0 ,'${'$'}options':'i'}},
                    {"name": {'${'$'}regex': ?0,'${'$'}options':'i' }}
                ]
            }
            
            """
    )
    fun queryLevelByKeyword(keyword: String): List<ArkLevel>

    /**
     * 根据stageId列表查询
     */
    fun findByStageIdIn(stageIds: Collection<String>): List<ArkLevel>
}
