package plus.maa.backend.service

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import plus.maa.backend.common.utils.ArkLevelUtil
import plus.maa.backend.common.utils.awaitString
import plus.maa.backend.repository.entity.gamedata.ArkActivity
import plus.maa.backend.repository.entity.gamedata.ArkCharacter
import plus.maa.backend.repository.entity.gamedata.ArkCrisisV2Info
import plus.maa.backend.repository.entity.gamedata.ArkStage
import plus.maa.backend.repository.entity.gamedata.ArkTower
import plus.maa.backend.repository.entity.gamedata.ArkZone
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * @author john180
 */
@Service
class ArkGameDataService(
    webClientBuilder: WebClient.Builder,
) {
    companion object {
        private const val ARK_RESOURCE_BASE = "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel"
        private const val ARK_STAGE = "$ARK_RESOURCE_BASE/stage_table.json"
        private const val ARK_ZONE = "$ARK_RESOURCE_BASE/zone_table.json"
        private const val ARK_ACTIVITY = "$ARK_RESOURCE_BASE/activity_table.json"
        private const val ARK_CHARACTER = "$ARK_RESOURCE_BASE/character_table.json"
        private const val ARK_TOWER = "$ARK_RESOURCE_BASE/climb_tower_table.json"
        private const val ARK_CRISIS_V2 = "$ARK_RESOURCE_BASE/crisis_v2_table.json"
    }

    private val webClient = webClientBuilder.build()
    private final val mapper = jacksonObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    private val stageMap = ConcurrentHashMap<String, ArkStage>()
    private val levelStageMap = ConcurrentHashMap<String, ArkStage>()
    private val zoneMap = ConcurrentHashMap<String, ArkZone>()
    private val zoneActivityMap = ConcurrentHashMap<String, ArkActivity>()
    private val arkCharacterMap = ConcurrentHashMap<String, ArkCharacter>()
    private val arkTowerMap = ConcurrentHashMap<String, ArkTower>()
    private val arkCrisisV2InfoMap = ConcurrentHashMap<String, ArkCrisisV2Info>()

    suspend fun syncGameData(): Boolean = coroutineScope {
        awaitAll(
            async { syncStage() },
            async { syncZone() },
            async { syncActivity() },
            async { syncCharacter() },
            async { syncTower() },
            async { syncCrisisV2Info() },
        ).all { it }
    }

    fun findStage(levelId: String, code: String, stageId: String): ArkStage? {
        val stage = levelStageMap[levelId.lowercase(Locale.getDefault())]
        if (stage != null && stage.code.equals(code, ignoreCase = true)) {
            return stage
        }
        return stageMap[stageId]
    }

    fun findZone(levelId: String, code: String, stageId: String): ArkZone? {
        val stage = findStage(levelId, code, stageId)
        if (stage == null) {
            log.error { "[DATA]stage不存在:$stageId, Level: $levelId" }
            return null
        }
        val zone = zoneMap[stage.zoneId]
        if (zone == null) {
            log.error { "[DATA]zone不存在:${stage.zoneId}, Level: $levelId" }
        }
        return zone
    }

    fun findTower(zoneId: String) = arkTowerMap[zoneId]

    fun findCharacter(characterId: String): ArkCharacter? {
        val ids = characterId.split("_")
        return arkCharacterMap[ids[ids.size - 1]]
    }

    fun findActivityByZoneId(zoneId: String) = zoneActivityMap[zoneId]

    /**
     * 通过 stageId 或者 seasonId 提取危机合约信息
     *
     * @param id stageId 或者 seasonId
     * @return 危机合约信息，包含合约名、开始时间、结束时间等
     */
    fun findCrisisV2InfoById(id: String?) = findCrisisV2InfoByKeyInfo(ArkLevelUtil.getKeyInfoById(id))

    /**
     * 通过地图系列的唯一标识提取危机合约信息
     *
     * @param keyInfo 地图系列的唯一标识
     * @return 危机合约信息，包含合约名、开始时间、结束时间等
     */
    fun findCrisisV2InfoByKeyInfo(keyInfo: String) = arkCrisisV2InfoMap[keyInfo]

    suspend fun syncStage(): Boolean = try {
        val stages = getTextAsEntity<ArkStageTable>(ARK_STAGE).stages
        stageMap.clear()
        stageMap.putAll(stages)
        levelStageMap.clear()
        stages.forEach { (_, s) ->
            s.levelId?.let { id -> levelStageMap[id.lowercase(Locale.getDefault())] = s }
        }
        log.info { "[DATA]获取stage数据成功, 共${levelStageMap.size}条" }
        true
    } catch (e: Exception) {
        log.error(e) { "[DATA]同步stage数据异常" }
        false
    }

    private data class ArkStageTable(val stages: Map<String, ArkStage>)

    suspend fun syncZone(): Boolean = try {
        val tmp = getTextAsEntity<ArkZoneTable>(ARK_ZONE).zones
        zoneMap.clear()
        zoneMap.putAll(tmp)
        log.info { "[DATA]获取zone数据成功, 共${zoneMap.size}条" }
        true
    } catch (e: Exception) {
        log.error(e) { "[DATA]同步zone数据异常" }
        false
    }

    private data class ArkZoneTable(val zones: Map<String, ArkZone>)

    suspend fun syncActivity(): Boolean = try {
        val table = getTextAsEntity<ArkActivityTable>(ARK_ACTIVITY)
        val actMap = table.basicInfo
        val tmp = mutableMapOf<String, ArkActivity>()
        table.zoneToActivity.forEach { (zoneId, actId) ->
            actMap[actId]?.also { act -> tmp[zoneId] = act }
        }
        zoneActivityMap.clear()
        zoneActivityMap.putAll(tmp)
        log.info { "[DATA]获取activity数据成功, 共${zoneActivityMap.size}条" }
        true
    } catch (e: Exception) {
        log.error(e) { "[DATA]同步activity数据异常" }
        false
    }

    private data class ArkActivityTable(val zoneToActivity: Map<String, String>, val basicInfo: Map<String, ArkActivity>)

    suspend fun syncCharacter(): Boolean = try {
        val characters = getTextAsEntity<Map<String, ArkCharacter>>(ARK_CHARACTER)
        val tmp = mutableMapOf<String, ArkCharacter>()
        characters.forEach { (id, c) ->
            val ids = id.split("_")
            if (ids.size != 3) return@forEach
            c.id = id
            tmp[ids[2]] = c
        }
        arkCharacterMap.clear()
        arkCharacterMap.putAll(tmp)
        log.info { "[DATA]获取character数据成功, 共${arkCharacterMap.size}条" }
        true
    } catch (e: Exception) {
        log.error(e) { "[DATA]同步character数据异常" }
        false
    }

    suspend fun syncTower(): Boolean = try {
        val table = getTextAsEntity<ArkTowerTable>(ARK_TOWER)
        arkTowerMap.clear()
        arkTowerMap.putAll(table.towers)
        log.info { "[DATA]获取tower数据成功, 共${arkTowerMap.size}条" }
        true
    } catch (e: Exception) {
        log.error(e) { "[DATA]同步tower数据异常" }
        false
    }

    private data class ArkTowerTable(val towers: Map<String, ArkTower>)

    suspend fun syncCrisisV2Info(): Boolean = try {
        val tmp = getTextAsEntity<CrisisV2Table>(ARK_CRISIS_V2)
            .seasonInfoDataMap
            .mapKeys { ArkLevelUtil.getKeyInfoById(it.key) }
        arkCrisisV2InfoMap.clear()
        arkCrisisV2InfoMap.putAll(tmp)
        log.info { "[DATA]获取crisisV2Info数据成功, 共${arkCrisisV2InfoMap.size}条" }
        true
    } catch (e: Exception) {
        log.error(e) { "[DATA]同步crisisV2Info数据异常" }
        false
    }

    private data class CrisisV2Table(val seasonInfoDataMap: Map<String, ArkCrisisV2Info>)

    /**
     * Fetch a resource as text, parse as json and convert it to entity.
     *
     * **GithubContents returns responses of text/html normally**
     */
    private suspend inline fun <reified T> getTextAsEntity(uri: String): T {
        val text = webClient.get().uri(uri).retrieve().awaitString()
        return mapper.readValue<T>(text)
    }
}
