package plus.maa.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import plus.maa.backend.common.utils.ArkLevelUtil;
import plus.maa.backend.common.utils.converter.ArkLevelConverter;
import plus.maa.backend.controller.response.copilot.ArkLevelInfo;
import plus.maa.backend.repository.ArkLevelRepository;
import plus.maa.backend.repository.GithubRepository;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.ArkLevelSha;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.repository.entity.gamedata.MaaArkStage;
import plus.maa.backend.repository.entity.github.GithubCommit;
import plus.maa.backend.repository.entity.github.GithubContent;
import plus.maa.backend.repository.entity.github.GithubTree;
import plus.maa.backend.repository.entity.github.GithubTrees;
import plus.maa.backend.service.model.ArkLevelType;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author dragove
 * created on 2022/12/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArkLevelService {
    /**
     * Github api调用token 从 <a href="https://github.com/settings/tokens">tokens</a> 获取
     */
    @Value("${maa-copilot.github.token:}")
    private String githubToken;
    /**
     * maa 主仓库，一般不变
     */
    @Value("${maa-copilot.github.repo:MaaAssistantArknights/MaaAssistantArknights/dev}")
    private String maaRepoAndBranch;
    /**
     * 地图数据所在路径
     */
    @Value("${maa-copilot.github.repo.tile.path:resource/Arknights-Tile-Pos}")
    private String tilePosPath;

    private final GithubRepository githubRepo;
    private final RedisCache redisCache;
    private final ArkLevelRepository arkLevelRepo;
    private final ArkLevelParserService parserService;
    private final ArkGameDataService gameDataService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient okHttpClient;
    private final ArkLevelConverter arkLevelConverter;

    private final List<String> bypassFileNames = List.of("overview.json");

    @Cacheable("arkLevels")
    public List<ArkLevelInfo> getArkLevelInfos() {
        return arkLevelRepo.findAll()
                .stream()
                .map(arkLevelConverter::convert)
                .collect(Collectors.toList());
    }

    @Cacheable("arkLevel")
    public ArkLevelInfo findByLevelIdFuzzy(String levelId) {
        ArkLevel level = arkLevelRepo.findByLevelIdFuzzy(levelId).findAny().orElse(null);
        return arkLevelConverter.convert(level);
    }


    public List<ArkLevelInfo> queryLevelByKeyword(String keyword) {
        List<ArkLevel> levels = arkLevelRepo.queryLevelByKeyword(keyword).collect(Collectors.toList());
        return arkLevelConverter.convert(levels);
    }

    /**
     * 地图数据更新任务
     */
    @Async
    public void runSyncLevelDataTask() {
        log.info("[LEVEL]开始同步地图数据");
        //获取地图文件夹最新的commit, 用于判断是否需要更新
        List<GithubCommit> commits = githubRepo.getCommits(githubToken);
        if (CollectionUtils.isEmpty(commits)) {
            log.info("[LEVEL]获取地图数据最新commit失败");
            return;
        }
        //与缓存的commit比较，如果相同则不更新
        GithubCommit commit = commits.get(0);
        String lastCommit = redisCache.getCacheLevelCommit();
        if (lastCommit != null && lastCommit.equals(commit.getSha())) {
            log.info("[LEVEL]地图数据已是最新");
            return;
        }
        //获取根目录文件列表
        GithubTrees trees;
        List<String> files = Arrays.stream(tilePosPath.split("/")).toList();
        trees = githubRepo.getTrees(githubToken, commit.getSha());
        //根据路径获取文件列表
        for (String file : files) {
            if (trees == null || CollectionUtils.isEmpty(trees.getTree())) {
                log.info("[LEVEL]地图数据获取失败");
                return;
            }
            GithubTree tree = trees.getTree().stream()
                    .filter(t -> t.getPath().equals(file) && t.getType().equals("tree"))
                    .findFirst()
                    .orElse(null);
            if (tree == null) {
                log.info("[LEVEL]地图数据获取失败, 未找到文件夹{}", file);
                return;
            }
            trees = githubRepo.getTrees(githubToken, tree.getSha());
        }
        if (trees == null || CollectionUtils.isEmpty(trees.getTree())) {
            log.info("[LEVEL]地图数据获取失败, 未找到文件夹{}", tilePosPath);
            return;
        }
        //根据后缀筛选地图文件列表
        List<GithubTree> levelTrees = trees.getTree().stream()
                .filter(t -> t.getType().equals("blob") && t.getPath().endsWith(".json"))
                .collect(Collectors.toList());
        log.info("[LEVEL]已发现{}份地图数据", levelTrees.size());

        //根据sha筛选无需更新的地图
        List<String> shaList = arkLevelRepo.findAllShaBy().stream().map(ArkLevelSha::getSha).toList();
        levelTrees.removeIf(t -> shaList.contains(t.getSha()));
        // 排除overview文件、肉鸽、训练关卡和 Guide? 不知道是啥
        levelTrees.removeIf(t -> t.getPath().equals("overview.json") ||
                t.getPath().contains("roguelike") ||
                t.getPath().startsWith("tr_") ||
                t.getPath().startsWith("guide_"));
        levelTrees.removeIf(t -> t.getPath().contains("roguelike"));
        log.info("[LEVEL]{}份地图数据需要更新", levelTrees.size());
        if (levelTrees.isEmpty()) {
            return;
        }
        //同步GameData仓库数据
        gameDataService.syncGameData();

        DownloadTask task = new DownloadTask(levelTrees.size(), (t) -> {
            //仅在全部下载任务成功后更新commit缓存
            if (t.isAllSuccess()) {
                redisCache.setCacheLevelCommit(commit.getSha());
            }
        });
        levelTrees.forEach(tree -> download(task, tree));
    }

    /**
     * 更新地图开放状态
     */
    public void updateLevelOpenStatus() {
        log.info("[LEVEL-OPEN-STATUS]准备更新地图开放状态");
        GithubContent stages = githubRepo.getContents(githubToken, "resource").stream()
                .filter(content -> content.isFile() && "stages.json".equals(content.getName()))
                .findFirst()
                .orElse(null);
        if (stages == null) {
            log.info("[LEVEL-OPEN-STATUS]地图开放状态数据不存在");
            return;
        }

        String lastStagesSha = redisCache.getCache("level:stages:sha", String.class);
        if (lastStagesSha != null && lastStagesSha.equals(stages.getSha())) {
            log.info("[LEVEL-OPEN-STATUS]地图开放状态已是最新");
            return;
        }

        log.info("[LEVEL-OPEN-STATUS]开始更新地图开放状态");
        // 就一个文件，直接在当前线程下载数据
        try (Response response = okHttpClient
                .newCall(new Request.Builder().url(stages.getDownloadUrl()).build())
                .execute()) {

            if (!response.isSuccessful() || response.body() == null) {
                log.error("[LEVEL-OPEN-STATUS]地图开放状态下载失败");
                return;
            }

            var body = response.body().charStream();
            List<MaaArkStage> stagesList = mapper.readValue(body, new TypeReference<>() {
            });

            Set<String> keyInfos = stagesList.stream()
                    .map(MaaArkStage::getStageId)
                    // 提取地图系列的唯一标识
                    .map(ArkLevelUtil::getKeyInfoById)
                    .collect(Collectors.toUnmodifiableSet());

            // 分页修改
            Pageable pageable = Pageable.ofSize(1000);
            Page<ArkLevel> arkLevelPage = arkLevelRepo.findAll(pageable);
            while (arkLevelPage.hasContent()) {

                arkLevelPage.stream()
                        // 不处理危机合约
                        .filter(arkLevel -> !ArkLevelType.RUNE.getDisplay().equals(arkLevel.getCatOne()))
                        .forEach(arkLevel -> {
                            // 只考虑地图系列的唯一标识
                            if (keyInfos.contains(ArkLevelUtil.getKeyInfoById(arkLevel.getStageId()))) {

                                arkLevel.setIsOpen(true);
                            } else if (arkLevel.getIsOpen() != null) {
                                // 数据可能存在部分缺失，因此地图此前必须被匹配过，才会认为其关闭
                                arkLevel.setIsOpen(false);
                            }
                        });

                arkLevelRepo.saveAll(arkLevelPage);

                if (!arkLevelPage.hasNext()) {
                    // 没有下一页了，跳出循环
                    break;
                }
                pageable = arkLevelPage.nextPageable();
                arkLevelPage = arkLevelRepo.findAll(pageable);
            }

            redisCache.setData("level:stages:sha", stages.getSha());
            log.info("[LEVEL-OPEN-STATUS]地图开放状态更新完成");
        } catch (Exception e) {
            log.error("[LEVEL-OPEN-STATUS]地图开放状态更新失败", e);
        }
    }

    public void updateCrisisV2OpenStatus() {
        log.info("[CRISIS-V2-OPEN-STATUS]准备更新危机合约开放状态");
        // 同步危机合约信息
        gameDataService.syncCrisisV2Info();

        // 分页修改
        Pageable pageable = Pageable.ofSize(1000);
        Page<ArkLevel> arkCrisisV2Page = arkLevelRepo.findAllByCatOne(ArkLevelType.RUNE.getDisplay(), pageable);

        // 获取当前时间
        Instant nowInstant = Instant.now();

        while (arkCrisisV2Page.hasContent()) {

            arkCrisisV2Page.forEach(arkCrisisV2 -> Optional
                    .ofNullable(gameDataService.findCrisisV2InfoById(arkCrisisV2.getStageId()))
                    .map(crisisV2Info -> Instant.ofEpochSecond(crisisV2Info.getEndTs()))
                    .ifPresent(endInstant -> arkCrisisV2.setIsOpen(endInstant.isAfter(nowInstant)))
            );

            arkLevelRepo.saveAll(arkCrisisV2Page);

            if (!arkCrisisV2Page.hasNext()) {
                // 没有下一页了，跳出循环
                break;
            }
            pageable = arkCrisisV2Page.nextPageable();
            arkCrisisV2Page = arkLevelRepo.findAllByCatOne(ArkLevelType.RUNE.getDisplay(), pageable);
        }
        log.info("[CRISIS-V2-OPEN-STATUS]危机合约开放状态更新完毕");
    }

    /**
     * 下载地图数据
     */
    private void download(DownloadTask task, GithubTree tree) {
        String fileName = URLEncoder.encode(tree.getPath(), StandardCharsets.UTF_8);
        if (bypassFileNames.contains(fileName)) {
            task.success();
            return;
        }
        String url = String.format("https://raw.githubusercontent.com/%s/%s/%s", maaRepoAndBranch, tilePosPath, fileName);
        okHttpClient.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("[LEVEL]下载地图数据失败:" + tree.getPath(), e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody rspBody = response.body()) {
                    if (!response.isSuccessful() || rspBody == null) {
                        task.fail();
                        log.error("[LEVEL]下载地图数据失败:" + tree.getPath());
                        return;
                    }
                    ArkTilePos tilePos = mapper.readValue(rspBody.string(), ArkTilePos.class);
                    ArkLevel level = parserService.parseLevel(tilePos, tree.getSha());
                    if (level == null) {
                        task.fail();
                        log.info("[LEVEL]地图数据解析失败:" + tree.getPath());
                        return;
                    } else if (level == ArkLevel.EMPTY) {
                        task.pass();
                        return;
                    }
                    arkLevelRepo.save(level);

                    task.success();
                    log.info("[LEVEL]下载地图数据 {} 成功, 进度{}/{}, 用时:{}s", tilePos.getName(), task.getCurrent(), task.getTotal(), task.getDuration());
                }
            }
        });
    }

    @Data
    @RequiredArgsConstructor
    private static class DownloadTask {
        private final long startTime = System.currentTimeMillis();
        private final AtomicInteger success = new AtomicInteger(0);
        private final AtomicInteger fail = new AtomicInteger(0);
        private final AtomicInteger pass = new AtomicInteger(0);
        private final int total;
        private final Consumer<DownloadTask> finishCallback;

        public void success() {
            success.incrementAndGet();
            checkFinish();
        }

        public void fail() {
            fail.incrementAndGet();
            checkFinish();
        }

        public void pass() {
            pass.incrementAndGet();
            checkFinish();
        }

        public int getCurrent() {
            return success.get() + fail.get() + pass.get();
        }

        public int getDuration() {
            return (int) (System.currentTimeMillis() - startTime) / 1000;
        }

        public boolean isAllSuccess() {
            return success.get() + pass.get() == total;
        }

        private void checkFinish() {
            if (success.get() + fail.get() + pass.get() != total) {
                return;
            }
            finishCallback.accept(this);
            log.info("[LEVEL]地图数据下载完成, 成功:{}, 失败:{}, 跳过:{} 总用时{}s", success.get(), fail.get(), pass.get(), getDuration());
        }
    }

}
