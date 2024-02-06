package plus.maa.backend.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import okhttp3.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.data.domain.Pageable
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.util.CollectionUtils
import plus.maa.backend.common.utils.ArkLevelUtil
import plus.maa.backend.common.utils.converter.ArkLevelConverter
import plus.maa.backend.controller.response.copilot.ArkLevelInfo
import plus.maa.backend.repository.ArkLevelRepository
import plus.maa.backend.repository.GithubRepository
import plus.maa.backend.repository.RedisCache
import plus.maa.backend.repository.entity.ArkLevel
import plus.maa.backend.repository.entity.ArkLevelSha
import plus.maa.backend.repository.entity.gamedata.ArkTilePos
import plus.maa.backend.repository.entity.gamedata.MaaArkStage
import plus.maa.backend.repository.entity.github.GithubContent
import plus.maa.backend.repository.entity.github.GithubTree
import plus.maa.backend.repository.entity.github.GithubTrees
import plus.maa.backend.service.model.ArkLevelType
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

private val log = KotlinLogging.logger {  }

/**
 * @author dragove
 * created on 2022/12/23
 */
@Service
class ArkLevelService(
    /**
     * GitHub api调用token 从 [tokens](https://github.com/settings/tokens) 获取
     */
    @Value("\${maa-copilot.github.token:}")
    private val githubToken: String,
    /**
     * maa 主仓库，一般不变
     */
    @Value("\${maa-copilot.github.repo:MaaAssistantArknights/MaaAssistantArknights/dev}")
    private val maaRepoAndBranch: String,
    /**
     * 地图数据所在路径
     */
    @Value("\${maa-copilot.github.repo.tile.path:resource/Arknights-Tile-Pos}")
    private val tilePosPath: String,

    private val githubRepo: GithubRepository,
    private val redisCache: RedisCache,
    private val arkLevelRepo: ArkLevelRepository,
    private val parserService: ArkLevelParserService,
    private val gameDataService: ArkGameDataService,
    private val mapper: ObjectMapper,
    private val okHttpClient: OkHttpClient,
    private val arkLevelConverter: ArkLevelConverter
) {
    private val bypassFileNames = listOf("overview.json")

    @get:Cacheable("arkLevelInfos")
    val arkLevelInfos: List<ArkLevelInfo>
        get() = arkLevelRepo.findAll()
            .map { arkLevel: ArkLevel? -> arkLevelConverter.convert(arkLevel) }
            .toList()

    @Cacheable("arkLevel")
    fun findByLevelIdFuzzy(levelId: String): ArkLevel? {
        return arkLevelRepo.findByLevelIdFuzzy(levelId).firstOrNull()
    }

    fun queryLevelInfosByKeyword(keyword: String): List<ArkLevelInfo> {
        val levels = arkLevelRepo.queryLevelByKeyword(keyword).toList()
        return arkLevelConverter.convert(levels)
    }

    /**
     * 地图数据更新任务
     */
    @Async
    fun runSyncLevelDataTask() {
        log.info { "[LEVEL]开始同步地图数据" }
        //获取地图文件夹最新的commit, 用于判断是否需要更新
        val commits = githubRepo.getCommits(githubToken)
        if (CollectionUtils.isEmpty(commits)) {
            log.info { "[LEVEL]获取地图数据最新commit失败" }
            return
        }
        //与缓存的commit比较，如果相同则不更新
        val commit = commits[0]
        val lastCommit = redisCache.cacheLevelCommit
        if (lastCommit != null && lastCommit == commit!!.sha) {
            log.info { "[LEVEL]地图数据已是最新" }
            return
        }
        //获取根目录文件列表
        var trees: GithubTrees?
        val files = Arrays.stream(tilePosPath.split("/".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()).toList()
        trees = githubRepo.getTrees(githubToken, commit!!.sha)
        //根据路径获取文件列表
        for (file in files) {
            if (trees == null || CollectionUtils.isEmpty(trees.tree)) {
                log.info { "[LEVEL]地图数据获取失败" }
                return
            }
            val tree = trees.tree.stream()
                .filter { t: GithubTree -> t.path == file && t.type == "tree" }
                .findFirst()
                .orElse(null)
            if (tree == null) {
                log.info { "[LEVEL]地图数据获取失败, 未找到文件夹$file" }
                return
            }
            trees = githubRepo.getTrees(githubToken, tree.sha)
        }
        if (trees == null || CollectionUtils.isEmpty(trees.tree)) {
            log.info { "[LEVEL]地图数据获取失败, 未找到文件夹$tilePosPath" }
            return
        }
        //根据后缀筛选地图文件列表
        val levelTrees = trees.tree
            .filter { t: GithubTree -> t.type == "blob" && t.path.endsWith(".json") }
            .toMutableList()
        log.info { "[LEVEL]已发现${levelTrees.size}份地图数据" }

        //根据sha筛选无需更新的地图
        val shaList = arkLevelRepo.findAllShaBy().stream().map { obj: ArkLevelSha -> obj.sha }.toList()
        levelTrees.removeIf { t: GithubTree -> shaList.contains(t.sha) }
        // 排除overview文件、肉鸽、训练关卡和 Guide? 不知道是啥
        levelTrees.removeIf { t: GithubTree ->
            t.path == "overview.json" ||
                    t.path.contains("roguelike") ||
                    t.path.startsWith("tr_") ||
                    t.path.startsWith("guide_")
        }
        levelTrees.removeIf { t: GithubTree -> t.path.contains("roguelike") }
        log.info { "[LEVEL]${levelTrees.size}份地图数据需要更新" }
        if (levelTrees.isEmpty()) {
            return
        }
        //同步GameData仓库数据
        if (!gameDataService.syncGameData()) {
            log.error { "[LEVEL]GameData仓库数据同步失败" }
            return
        }

        val task = DownloadTask(total = levelTrees.size, finishCallback = { t: DownloadTask ->
            //仅在全部下载任务成功后更新commit缓存
            if (t.isAllSuccess) {
                redisCache.cacheLevelCommit = commit.sha
            }
        })
        levelTrees.forEach { tree -> download(task, tree) }
    }

    /**
     * 更新活动地图开放状态
     */
    fun updateActivitiesOpenStatus() {
        log.info { "[ACTIVITIES-OPEN-STATUS]准备更新活动地图开放状态" }
        val stages = githubRepo.getContents(githubToken, "resource").stream()
            .filter { content: GithubContent -> content.isFile && "stages.json" == content.name }
            .findFirst()
            .orElse(null)
        if (stages == null) {
            log.info { "[ACTIVITIES-OPEN-STATUS]活动地图开放状态数据不存在" }
            return
        }

        val lastStagesSha = redisCache.getCache("level:stages:sha", String::class.java)
        if (lastStagesSha != null && lastStagesSha == stages.sha) {
            log.info { "[ACTIVITIES-OPEN-STATUS]活动地图开放状态已是最新" }
            return
        }

        log.info { "[ACTIVITIES-OPEN-STATUS]开始更新活动地图开放状态" }
        // 就一个文件，直接在当前线程下载数据
        try {
            okHttpClient
                .newCall(Request.Builder().url(stages.downloadUrl).build())
                .execute().use { response ->
                    if (!response.isSuccessful || response.body == null) {
                        log.error { "[ACTIVITIES-OPEN-STATUS]活动地图开放状态下载失败" }
                        return
                    }
                    val body = response.body!!.byteStream()
                    val stagesList: List<MaaArkStage> =
                        mapper.readValue(body, object : TypeReference<List<MaaArkStage>>() {
                        })

                    val keyInfos = stagesList
                        .map { it.stageId } // 提取地图系列的唯一标识
                        .map { id: String? -> ArkLevelUtil.getKeyInfoById(id) }
                        .toSet()

                    // 修改活动地图
                    val catOne = ArkLevelType.ACTIVITIES.display
                    // 分页修改
                    var pageable = Pageable.ofSize(1000)
                    var arkLevelPage = arkLevelRepo.findAllByCatOne(catOne, pageable)

                    // 获取当前时间
                    val nowTime = LocalDateTime.now()

                    while (arkLevelPage.hasContent()) {
                        arkLevelPage.forEach{ arkLevel: ArkLevel ->
                            // 只考虑地图系列的唯一标识
                            if (keyInfos.contains(ArkLevelUtil.getKeyInfoById(arkLevel.stageId))) {
                                arkLevel.isOpen = true
                                // 如果一个旧地图重新开放，关闭时间也需要另算
                                arkLevel.closeTime = null
                            } else if (arkLevel.isOpen != null) {
                                // 数据可能存在部分缺失，因此地图此前必须被匹配过，才会认为其关闭
                                arkLevel.isOpen = false
                                // 不能每天都变更关闭时间
                                if (arkLevel.closeTime == null) {
                                    arkLevel.closeTime = nowTime
                                }
                            }
                        }

                        arkLevelRepo.saveAll(arkLevelPage)

                        if (!arkLevelPage.hasNext()) {
                            // 没有下一页了，跳出循环
                            break
                        }
                        pageable = arkLevelPage.nextPageable()
                        arkLevelPage = arkLevelRepo.findAllByCatOne(catOne, pageable)
                    }

                    redisCache.setData("level:stages:sha", stages.sha)
                    log.info { "[ACTIVITIES-OPEN-STATUS]活动地图开放状态更新完成" }
                }
        } catch (e: Exception) {
            log.error(e) { "[ACTIVITIES-OPEN-STATUS]活动地图开放状态更新失败" }
        }
    }

    fun updateCrisisV2OpenStatus() {
        log.info { "[CRISIS-V2-OPEN-STATUS]准备更新危机合约开放状态" }
        // 同步危机合约信息
        if (!gameDataService.syncCrisisV2Info()) {
            log.error { "[CRISIS-V2-OPEN-STATUS]同步危机合约信息失败" }
            return
        }

        val catOne = ArkLevelType.RUNE.display
        // 分页修改
        var pageable = Pageable.ofSize(1000)
        var arkCrisisV2Page = arkLevelRepo.findAllByCatOne(catOne, pageable)

        // 获取当前时间
        val nowInstant = Instant.now()
        val nowTime = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault())

        while (arkCrisisV2Page.hasContent()) {
            arkCrisisV2Page.forEach { arkCrisisV2: ArkLevel ->
                // 危机合约信息比较准，因此未匹配一律视为已关闭
                arkCrisisV2.isOpen = false
                gameDataService.findCrisisV2InfoById(arkCrisisV2.stageId)?.let { crisisV2Info ->
                    val instant = Instant.ofEpochSecond(crisisV2Info.endTs)
                    arkCrisisV2.isOpen = instant.isAfter(nowInstant)
                }
                if (arkCrisisV2.closeTime == null && java.lang.Boolean.FALSE == arkCrisisV2.isOpen) {
                    // 危机合约应该不存在赛季重新开放的问题，只要不每天变动关闭时间即可
                    arkCrisisV2.closeTime = nowTime
                }
            }

            arkLevelRepo.saveAll(arkCrisisV2Page)

            if (!arkCrisisV2Page.hasNext()) {
                // 没有下一页了，跳出循环
                break
            }
            pageable = arkCrisisV2Page.nextPageable()
            arkCrisisV2Page = arkLevelRepo.findAllByCatOne(catOne, pageable)
        }
        log.info { "[CRISIS-V2-OPEN-STATUS]危机合约开放状态更新完毕" }
    }

    /**
     * 下载地图数据
     */
    private fun download(task: DownloadTask, tree: GithubTree) {
        val fileName = URLEncoder.encode(tree.path, StandardCharsets.UTF_8)
        if (bypassFileNames.contains(fileName)) {
            task.success()
            return
        }
        val url = String.format("https://raw.githubusercontent.com/%s/%s/%s", maaRepoAndBranch, tilePosPath, fileName)
        okHttpClient.newCall(Request.Builder().url(url).build()).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                log.error(e) { "[LEVEL]下载地图数据失败:" + tree.path }
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                response.body.use { rspBody ->
                    if (!response.isSuccessful || rspBody == null) {
                        task.fail()
                        log.error { "[LEVEL]下载地图数据失败:" + tree.path }
                        return
                    }
                    val tilePos = mapper.readValue(rspBody.string(), ArkTilePos::class.java)
                    val level = parserService.parseLevel(tilePos, tree.sha)
                    if (level == null) {
                        task.fail()
                        log.info { "[LEVEL]地图数据解析失败:" + tree.path }
                        return
                    } else if (level === ArkLevel.EMPTY) {
                        task.pass()
                        return
                    }
                    arkLevelRepo.save(level)

                    task.success()
                    log.info { "[LEVEL]下载地图数据 ${tilePos.name} 成功, 进度${task.current}/${task.total}, 用时:${task.duration}s" }
                }
            }
        })
    }

    private class DownloadTask(
        private val startTime: Long = System.currentTimeMillis(),
        private val success: AtomicInteger = AtomicInteger(0),
        private val fail: AtomicInteger = AtomicInteger(0),
        private val pass: AtomicInteger = AtomicInteger(0),
        val total: Int = 0,
        private val finishCallback: ((DownloadTask) -> Unit)? = null
    ) {
        fun success() {
            success.incrementAndGet()
            checkFinish()
        }

        fun fail() {
            fail.incrementAndGet()
            checkFinish()
        }

        fun pass() {
            pass.incrementAndGet()
            checkFinish()
        }

        val current: Int
            get() = success.get() + fail.get() + pass.get()

        val duration: Int
            get() = (System.currentTimeMillis() - startTime).toInt() / 1000

        val isAllSuccess: Boolean
            get() = success.get() + pass.get() == total

        private fun checkFinish() {
            if (success.get() + fail.get() + pass.get() != total) {
                return
            }
            finishCallback!!.invoke(this)
            log.info { "[LEVEL]地图数据下载完成, 成功:${success.get()}, 失败:${fail.get()}, 跳过:${pass.get()} 总用时${duration}s" }
        }
    }
}
