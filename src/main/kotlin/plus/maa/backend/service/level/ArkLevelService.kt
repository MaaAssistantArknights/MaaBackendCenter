package plus.maa.backend.service.level

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.DefaultUriBuilderFactory
import plus.maa.backend.common.utils.awaitString
import plus.maa.backend.common.utils.converter.ArkLevelConverter
import plus.maa.backend.common.utils.lazySuspend
import plus.maa.backend.config.external.MaaCopilotProperties
import plus.maa.backend.controller.response.copilot.ArkLevelInfo
import plus.maa.backend.repository.ArkLevelRepository
import plus.maa.backend.repository.GithubRepository
import plus.maa.backend.repository.RedisCache
import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.gamedata.ArkTilePos
import plus.maa.backend.repository.entity.gamedata.MaaArkStage
import plus.maa.backend.repository.entity.github.GithubCommit
import plus.maa.backend.repository.entity.github.GithubTree
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author dragove
 * created on 2022/12/23
 */
@Service
class ArkLevelService(
    properties: MaaCopilotProperties,
    private val githubRepo: GithubRepository,
    private val redisCache: RedisCache,
    private val arkLevelRepo: ArkLevelRepository,
    private val mapper: ObjectMapper,
    private val arkLevelConverter: ArkLevelConverter,
    webClientBuilder: WebClient.Builder,
) {
    private val log = KotlinLogging.logger { }
    private val github = properties.github
    private val webClient = webClientBuilder
        .uriBuilderFactory(DefaultUriBuilderFactory().apply { encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE })
        .build()
    private val fetchDataHolder = lazySuspend { ArkGameDataHolder.fetch(webClient) }
    private val fetchLevelParser = lazySuspend { ArkLevelParserDelegate(fetchDataHolder()) }

    @get:Cacheable("arkLevelInfos")
    val arkLevelInfos: List<ArkLevelInfo>
        get() = arkLevelRepo.findAll().map { arkLevel -> arkLevelConverter.convert(arkLevel) }

    @Cacheable("arkLevel")
    fun findByLevelIdFuzzy(levelId: String): ArkLevel? = arkLevelRepo.findByLevelIdFuzzy(levelId).firstOrNull()

    fun queryLevelInfosByKeyword(keyword: String): List<ArkLevelInfo> {
        val levels = arkLevelRepo.queryLevelByKeyword(keyword)
        return arkLevelConverter.convert(levels)
    }

    /**
     * 地图数据更新任务
     */
    suspend fun syncLevelData() {
        val tag = "[LEVEL]"
        try {
            log.info { "${tag}开始同步地图数据" }
            // 获取地图文件夹最新的 commit, 与缓存的 commit 比较，如果相同则不更新
            val commit = getGithubCommits().getOrNull(0)
            checkNotNull(commit) { "获取地图数据最新 commit 失败" }
            if (redisCache.cacheLevelCommit == commit.sha) {
                log.info { "${tag}地图数据已是最新" }
                return
            }

            val trees = fetchTilePosGithubTreesToUpdate(commit, github.tilePosPath)
            log.info { "${tag}已发现 ${trees.size} 份地图数据" }

            // 根据 sha 筛选无需更新的地图
            val shaList = withContext(Dispatchers.IO) { arkLevelRepo.findAllShaBy() }.map { it.sha }
            val filtered = trees.filter { !shaList.contains(it.sha) }

            val parser = fetchLevelParser()
            val allSuccess = downloadAndSaveLevelDatum(filtered, parser)
            if (allSuccess) redisCache.cacheLevelCommit = commit.sha
        } catch (e: Exception) {
            log.error(e) { "${tag}同步地图数据失败" }
        }
    }

    private suspend fun fetchTilePosGithubTreesToUpdate(commit: GithubCommit, path: String): List<GithubTree> {
        var folder = getGithubTree(commit.sha)
        val pathSegments = path.split("/").filter(String::isNotEmpty)
        for (seg in pathSegments) {
            val targetTree = folder.tree.firstOrNull { it.path == seg && it.type == "tree" }
                ?: throw Exception("[LEVEL]地图数据获取失败, 未找到文件夹 $path")
            folder = getGithubTree(targetTree.sha)
        }
        // 根据后缀筛选地图文件列表,排除 overview 文件、肉鸽、训练关卡和 Guide? 不知道是啥
        return folder.tree.filter {
            it.type == "blob" &&
                it.path.endsWith(".json") &&
                it.path != "overview.json" &&
                !it.path.contains("roguelike") &&
                !it.path.startsWith("tr_") &&
                !it.path.startsWith("guide_")
        }
    }

    private suspend fun downloadAndSaveLevelDatum(trees: List<GithubTree>, parser: ArkLevelParserDelegate): Boolean {
        val total = trees.size
        val success = AtomicInteger(0)
        val fail = AtomicInteger(0)
        val pass = AtomicInteger(0)
        val startTime = System.currentTimeMillis()

        fun current() = success.get() + fail.get() + pass.get()
        fun duration() = (System.currentTimeMillis() - startTime) / 1000
        fun formatLog(path: String, result: String) = "[LEVEL][${current()}/$total][${duration()}s] 更新 $path $result"

        suspend fun downloadAndSave(tree: GithubTree) = try {
            val fileName = URLEncoder.encode(tree.path, StandardCharsets.UTF_8)
            val url = "https://raw.githubusercontent.com/${github.repoAndBranch}/${github.tilePosPath}/$fileName"
            val tilePos = getTextAsEntity<ArkTilePos>(url)
            val level = parser.parseLevel(tilePos, tree.sha)
            checkNotNull(level) { "地图数据解析失败: ${tree.path}" }
            if (level === ArkLevel.EMPTY) {
                pass.incrementAndGet()
                log.info { formatLog(tree.path, "跳过") }
            } else {
                withContext(Dispatchers.IO) { arkLevelRepo.save(level) }
                success.incrementAndGet()
                log.info { formatLog(tree.path, "成功") }
            }
        } catch (e: Exception) {
            fail.incrementAndGet()
            log.error(e) { formatLog(tree.path, "失败") }
        }

        log.info { "[LEVEL] $total 份地图数据需要更新" }
        coroutineScope {
            // 每次最多同时请求 200 个
            trees.chunked(200).forEach { chunk ->
                chunk.map { async { downloadAndSave(it) } }.awaitAll()
            }
        }
        log.info { "[LEVEL]地图数据更新完成, 成功:${success.get()}, 失败:${fail.get()}, 跳过:${pass.get()} 总用时 ${duration()}s" }
        return success.get() + pass.get() == total
    }

    /**
     * 更新活动地图开放状态
     */
    suspend fun updateActivitiesOpenStatus() {
        val cacheKey = "level:stages:sha"
        try {
            log.info { "[ACTIVITIES-OPEN-STATUS]准备更新活动地图开放状态" }
            val content = getGithubContent("resource")
                .firstOrNull { it.isFile && "stages.json" == it.name }

            if (content?.downloadUrl == null) {
                log.info { "[ACTIVITIES-OPEN-STATUS]活动地图开放状态数据不存在" }
                return
            }

            val lastSha = redisCache.getCache(cacheKey, String::class.java)
            if (lastSha == content.sha) {
                log.info { "[ACTIVITIES-OPEN-STATUS]活动地图开放状态已是最新" }
                return
            }
            val openStageKeyInfos = getTextAsEntity<List<MaaArkStage>>(content.downloadUrl)
                .map { ArkLevelUtil.getKeyInfoById(it.stageId) }
                .toSet()

            log.info { "[ACTIVITIES-OPEN-STATUS]开始更新活动地图开放状态" }

            val now = LocalDateTime.now()
            updateLevelsOfTypeInBatch(ArkLevelType.ACTIVITIES) { arkLevel: ArkLevel ->
                val currentOpen = ArkLevelUtil.getKeyInfoById(arkLevel.stageId) in openStageKeyInfos
                if (currentOpen) {
                    arkLevel.closeTime = null
                } else if (arkLevel.isOpen == true) {
                    // 确认已经被关闭: 曾经开放，现在不开放
                    arkLevel.closeTime = arkLevel.closeTime ?: now
                }
                arkLevel.isOpen = currentOpen
            }

            redisCache.setData(cacheKey, content.sha)
            log.info { "[ACTIVITIES-OPEN-STATUS]活动地图开放状态更新完成" }
        } catch (e: Exception) {
            log.error(e) { "[ACTIVITIES-OPEN-STATUS]活动地图开放状态更新失败" }
        }
    }

    /**
     * 更新危机合约开放状态
     */
    suspend fun updateCrisisV2OpenStatus() {
        log.info { "[CRISIS-V2-OPEN-STATUS]准备更新危机合约开放状态" }
        val holder = fetchDataHolder().updateCrisisV2Info()
        val nowTime = LocalDateTime.now()

        updateLevelsOfTypeInBatch(ArkLevelType.RUNE) { level ->
            val info = holder.findCrisisV2InfoById(level.stageId) ?: return@updateLevelsOfTypeInBatch
            level.closeTime = LocalDateTime.ofEpochSecond(info.endTs, 0, ZoneOffset.UTC)
            level.isOpen = level.closeTime?.isAfter(nowTime)
        }
        log.info { "[CRISIS-V2-OPEN-STATUS]危机合约开放状态更新完毕" }
    }

    suspend fun updateLevelsOfTypeInBatch(catOne: ArkLevelType, batchSize: Int = 1000, block: (ArkLevel) -> Unit) {
        var pageable = Pageable.ofSize(batchSize)
        do {
            val page = withContext(Dispatchers.IO) { arkLevelRepo.findAllByCatOne(catOne.display, pageable) }
            page.forEach(block)
            withContext(Dispatchers.IO) { arkLevelRepo.saveAll(page) }
            pageable = page.nextPageable()
        } while (page.hasNext())
    }

    private suspend fun getGithubCommits() = withContext(Dispatchers.IO) { githubRepo.getCommits(github.token) }
    private suspend fun getGithubTree(sha: String) = withContext(Dispatchers.IO) { githubRepo.getTrees(github.token, sha) }
    private suspend fun getGithubContent(path: String) = withContext(Dispatchers.IO) { githubRepo.getContents(github.token, path) }

    /**
     * Fetch a resource as text, parse as json and convert it to entity.
     *
     * GithubContents returns responses of `text/html` normally
     */
    private suspend inline fun <reified T> getTextAsEntity(uri: String): T {
        val text = webClient.get().uri(uri).retrieve().awaitString()
        return mapper.readValue<T>(text)
    }
}
