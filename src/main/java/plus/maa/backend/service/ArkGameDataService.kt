package plus.maa.backend.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service
import plus.maa.backend.common.utils.ArkLevelUtil
import plus.maa.backend.repository.entity.gamedata.*
import java.util.*
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * @author john180
 */
@Service
class ArkGameDataService(private val okHttpClient: OkHttpClient) {

    companion object {
        private const val ARK_STAGE =
            "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/stage_table.json"
        private const val ARK_ZONE =
            "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/zone_table.json"
        private const val ARK_ACTIVITY =
            "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/activity_table.json"
        private const val ARK_CHARACTER =
            "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/character_table.json"
        private const val ARK_TOWER =
            "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/climb_tower_table.json"
        private const val ARK_CRISIS_V2 =
            "https://raw.githubusercontent.com/yuanyan3060/ArknightsGameResource/main/gamedata/excel/crisis_v2_table.json"
    }

    private final val mapper = jacksonMapperBuilder()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        .build()
    private val stageMap = ConcurrentHashMap<String, ArkStage>()
    private val levelStageMap = ConcurrentHashMap<String, ArkStage>()
    private val zoneMap = ConcurrentHashMap<String, ArkZone>()
    private val zoneActivityMap = ConcurrentHashMap<String, ArkActivity>()
    private val arkCharacterMap = ConcurrentHashMap<String, ArkCharacter>()
    private val arkTowerMap = ConcurrentHashMap<String, ArkTower>()
    private val arkCrisisV2InfoMap = ConcurrentHashMap<String, ArkCrisisV2Info>()

    fun syncGameData(): Boolean {
        return syncStage() &&
                syncZone() &&
                syncActivity() &&
                syncCharacter() &&
                syncTower() &&
                syncCrisisV2Info()
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
            log.error { "${"[DATA]stage不存在:{}, Level: {}"} $stageId $levelId" }
            return null
        }
        val zone = zoneMap[stage.zoneId]
        if (zone == null) {
            log.error { "${"[DATA]zone不存在:{}, Level: {}"} ${stage.zoneId} $levelId" }
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

    private fun syncStage(): Boolean {
        val req = Request.Builder().url(ARK_STAGE).get().build()
        try {
            okHttpClient.newCall(req).execute().use { rsp ->
                val body = rsp.body
                if (body == null) {
                    log.error { "[DATA]获取stage数据失败" }
                    return false
                }
                val node = mapper.reader().readTree(body.string())
                val stagesNode = node.get("stages")
                val temp = mapper.convertValue(stagesNode, object : TypeReference<Map<String, ArkStage>>() {})
                stageMap.clear()
                levelStageMap.clear()
                temp.forEach { (k, v) ->
                    stageMap[k] = v
                    v.levelId?.let {
                        levelStageMap[it.lowercase(Locale.getDefault())] = v
                    }
                }
                log.info { "${"[DATA]获取stage数据成功, 共{}条"} ${levelStageMap.size}" }
            }
        } catch (e: Exception) {
            log.error(e) { "[DATA]同步stage数据异常" }
            return false
        }
        return true
    }

    private fun syncZone(): Boolean {
        val req = Request.Builder().url(ARK_ZONE).get().build()
        try {
            okHttpClient.newCall(req).execute().use { rsp ->
                val body = rsp.body
                if (body == null) {
                    log.error { "[DATA]获取zone数据失败" }
                    return false
                }
                val node = mapper.reader().readTree(body.string())
                val zonesNode = node.get("zones")
                val temp = mapper.convertValue(zonesNode, object : TypeReference<Map<String, ArkZone>>() {})
                zoneMap.clear()
                zoneMap.putAll(temp)
                log.info { "${"[DATA]获取zone数据成功, 共{}条"} ${zoneMap.size}" }
            }
        } catch (e: Exception) {
            log.error(e) { "[DATA]同步zone数据异常" }
            return false
        }

        return true
    }

    private fun syncActivity(): Boolean {
        val req = Request.Builder().url(ARK_ACTIVITY).get().build()
        try {
            okHttpClient.newCall(req).execute().use { rsp ->
                val body = rsp.body
                if (body == null) {
                    log.error { "[DATA]获取activity数据失败" }
                    return false
                }
                val node = mapper.reader().readTree(body.string())
                val zonesNode = node.get("zoneToActivity")
                val zoneToActivity = mapper.convertValue(zonesNode, object : TypeReference<Map<String, String>>() {})
                val baseInfoNode = node.get("basicInfo")
                val baseInfos = mapper.convertValue(baseInfoNode, object : TypeReference<Map<String, ArkActivity>>() {})
                val temp = ConcurrentHashMap<String, ArkActivity>()
                zoneToActivity.forEach { (zoneId, actId) ->
                    val act = baseInfos[actId]
                    act?.let {
                        temp[zoneId] = it
                    }
                }
                zoneActivityMap.clear()
                zoneActivityMap.putAll(temp)
                log.info { "${"[DATA]获取activity数据成功, 共{}条"} ${zoneActivityMap.size}" }
            }
        } catch (e: Exception) {
            log.error(e) { "[DATA]同步activity数据异常" }
            return false
        }

        return true
    }

    private fun syncCharacter(): Boolean {
        val req = Request.Builder().url(ARK_CHARACTER).get().build()
        try {
            okHttpClient.newCall(req).execute().use { rsp ->
                val body = rsp.body
                if (body == null) {
                    log.error { "[DATA]获取character数据失败" }
                    return false
                }
                val node = mapper.reader().readTree(body.string())
                val characters = mapper.convertValue(node, object : TypeReference<Map<String, ArkCharacter>>() {})
                characters.forEach { (id, c) -> c.id = id }
                arkCharacterMap.clear()
                characters.values.forEach { c ->
                    if (c.id.isNullOrBlank()) {
                        return@forEach
                    }
                    val ids = c.id!!.split("_")
                    if (ids.size != 3) {
                        // 不是干员
                        return@forEach
                    }
                    arkCharacterMap[ids[2]] = c
                }
                log.info { "${"[DATA]获取character数据成功, 共{}条"} ${arkCharacterMap.size}" }
            }
        } catch (e: Exception) {
            log.error(e) { "[DATA]同步character数据异常" }
            return false
        }

        return true
    }

    private fun syncTower(): Boolean {
        val req = Request.Builder().url(ARK_TOWER).get().build()
        try {
            okHttpClient.newCall(req).execute().use { rsp ->
                val body = rsp.body
                if (body == null) {
                    log.error { "[DATA]获取tower数据失败" }
                    return false
                }
                val node = mapper.reader().readTree(body.string())
                val towerNode = node.get("towers")
                arkTowerMap.clear()
                arkTowerMap.putAll(mapper.convertValue(towerNode, object : TypeReference<Map<String, ArkTower>>() {}))
                log.info { "${"[DATA]获取tower数据成功, 共{}条"} ${arkTowerMap.size}" }
            }
        } catch (e: Exception) {
            log.error(e) { "[DATA]同步tower数据异常" }
            return false
        }

        return true
    }

    fun syncCrisisV2Info(): Boolean {
        val req = Request.Builder().url(ARK_CRISIS_V2).get().build()
        try {
            okHttpClient.newCall(req).execute().use { rsp ->
                val body = rsp.body
                if (body == null) {
                    log.error { "[DATA]获取crisisV2Info数据失败" }
                    return false
                }
                val node = mapper.reader().readTree(body.string())
                val crisisV2InfoNode = node.get("seasonInfoDataMap")
                val crisisV2InfoMap =
                    mapper.convertValue(crisisV2InfoNode, object : TypeReference<Map<String, ArkCrisisV2Info>>() {})
                val temp = ConcurrentHashMap<String, ArkCrisisV2Info>()
                crisisV2InfoMap.forEach { (k, v) -> temp[ArkLevelUtil.getKeyInfoById(k)] = v }
                arkCrisisV2InfoMap.clear()
                arkCrisisV2InfoMap.putAll(temp)
                log.info { "${"[DATA]获取crisisV2Info数据成功, 共{}条"} ${arkCrisisV2InfoMap.size}" }
            }
        } catch (e: Exception) {
            log.error(e) { "[DATA]同步crisisV2Info数据异常" }
            return false
        }

        return true
    }
}
