package plus.maa.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import plus.maa.backend.common.utils.converter.ArkLevelConverter;
import plus.maa.backend.controller.response.copilot.ArkLevelInfo;
import plus.maa.backend.repository.ArkLevelRepository;
import plus.maa.backend.repository.GithubRepository;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.ArkLevelSha;
import plus.maa.backend.repository.entity.gamedata.ArkTilePos;
import plus.maa.backend.repository.entity.github.GithubCommit;
import plus.maa.backend.repository.entity.github.GithubTree;
import plus.maa.backend.repository.entity.github.GithubTrees;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
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
    @Value("${maa-copilot.github.repo:MaaAssistantArknights/MaaAssistantArknights}")
    private String maaRepo;
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
     * 下载地图数据
     */
    private void download(DownloadTask task, GithubTree tree) {
        String fileName = URLEncoder.encode(tree.getPath(), StandardCharsets.UTF_8);
        if (bypassFileNames.contains(fileName)) {
            task.success();
            return;
        }
        String url = String.format("https://raw.githubusercontent.com/%s/master/%s/%s", maaRepo, tilePosPath, fileName);
        okHttpClient.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("[LEVEL]下载地图数据失败:" + tree.getPath(), e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody rspBody = response.body()) {
                    if (!response.isSuccessful()) {
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
