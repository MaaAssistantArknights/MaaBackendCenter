package plus.maa.backend.service;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
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

    @Value("${maa-copilot.ark-level-git.repository:}")
    private String gitRepository;
    @Value("${maa-copilot.ark-level-git.local-repository:}")
    private String localRepository;
    @Value("${maa-copilot.ark-level-git.json-path:}")
    private String jsonPath;
    private final GithubRepository githubRepo;
    private final RedisCache redisCache;
    private final ArkLevelRepository arkLevelRepo;
    private final ArkLevelParserService parserService;
    private final ArkGameDataService gameDataService;
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient okHttpClient;

    @Cacheable("arkLevels")
    public List<ArkLevelInfo> getArkLevelInfos() {
        return arkLevelRepo.findAll()
                .stream()
                .map(ArkLevelConverter.INSTANCE::convert)
                .collect(Collectors.toList());
    }

    @Cacheable("arkLevel")
    public ArkLevelInfo findByLevelIdFuzzy(String levelId) {
        ArkLevel level = arkLevelRepo.findByLevelIdFuzzy(levelId).findAny().orElse(null);
        return ArkLevelConverter.INSTANCE.convert(level);
    }


    public List<ArkLevelInfo> queryLevelByKeyword(String keyword) {
        List<ArkLevel> levels = arkLevelRepo.queryLevelByKeyword(keyword).collect(Collectors.toList());
        return ArkLevelConverter.INSTANCE.convert(levels);
    }

    /**
     * 地图数据更新任务
     */
    @Async
    public void runSyncLevelDataTask() {
        log.info("[LEVEL]开始同步地图数据");
        //获取地图文件夹最新的commit, 用于判断是否需要更新
        List<GithubCommit> commits = githubRepo.getCommits(githubToken, maaRepo, tilePosPath, 1);
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
        trees = githubRepo.getTrees(githubToken, maaRepo, commit.getSha());
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
            trees = githubRepo.getTrees(githubToken, maaRepo, tree.getSha());
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
        String url = String.format("https://raw.githubusercontent.com/%s/master/%s/%s", maaRepo, tilePosPath, fileName);
        okHttpClient.newCall(new Request.Builder().url(url).build()).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("[LEVEL]下载地图数据失败:" + tree.getPath(), e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    task.fail();
                    log.error("[LEVEL]下载地图数据失败:" + tree.getPath());
                    return;
                }
                ResponseBody body = response.body();
                if (body == null) {
                    task.fail();
                    log.error("[LEVEL]下载地图数据失败:" + tree.getPath());
                    return;
                }
                ArkTilePos tilePos = mapper.readValue(body.string(), ArkTilePos.class);

                ArkLevel level = parserService.parseLevel(tilePos, tree.getSha());
                if (level == null) {
                    task.fail();
                    log.info("[LEVEL]地图数据解析失败:" + tree.getPath());
                    return;
                }
                arkLevelRepo.save(level);

                task.success();
                log.info("[LEVEL]下载地图数据 {} 成功, 进度{}/{}, 用时:{}s", tilePos.getName(), task.getCurrent(), task.getTotal(), task.getDuration());
            }
        });
    }

    @Data
    @RequiredArgsConstructor
    private static class DownloadTask {
        private final long startTime = System.currentTimeMillis();
        private final AtomicInteger success = new AtomicInteger(0);
        private final AtomicInteger fail = new AtomicInteger(0);
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

        public int getCurrent() {
            return success.get() + fail.get();
        }

        public int getDuration() {
            return (int) (System.currentTimeMillis() - startTime) / 1000;
        }

        public boolean isAllSuccess() {
            return success.get() == total;
        }

        private void checkFinish() {
            if (success.get() + fail.get() != total) {
                return;
            }
            finishCallback.accept(this);
            log.info("[LEVEL]地图数据下载完成, 成功:{}, 失败:{}, 总用时{}s", success.get(), fail.get(), getDuration());
        }
    }

    @Async
    public void runSyncLevelDataTaskForJGit() {
        log.info("[LEVEL]开始同步地图数据");
        Git git = getGitRepository();
        if (git == null) {
            return;
        }
        String lastCommit;
        try {
            lastCommit = git.log().setMaxCount(1).call().iterator().next().getName();
        } catch (Exception exception) {
            exception.printStackTrace();
            log.warn("[LEVEL]获取最新commit失败");
            return;
        }
        String cacheCommit = redisCache.getCacheLevelCommit();
        if (Objects.equals(cacheCommit, lastCommit)) {
            log.info("[LEVEL]地图数据已是最新");
            return;
        }
        if (!gitCheckout(git, lastCommit)) {
            return;
        }

        File file = new File(Path.of(localRepository, jsonPath).toUri());
        if (!file.exists()) {
            log.warn("[LEVEL]地图数据获取失败, 未找到文件夹{}", jsonPath);
            return;
        }
        List<File> jsons = Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(f -> f.isFile() && f.getName().endsWith(".json")).toList();
        log.info("[LEVEL]已发现{}份地图数据", jsons.size());

        List<String> shaList = arkLevelRepo.findAllShaBy().stream().map(ArkLevelSha::getSha).toList();
        try (ObjectInserter inserter = git.getRepository().newObjectInserter()) {

            int errorCount = 0;
            int handleCount = 0;
            for (File ignored : jsons) {
                try {
                    byte[] bytes = Files.readAllBytes(file.toPath());
                    String sha = inserter.idFor(Constants.OBJ_BLOB, bytes).name();
                    if (shaList.contains(sha)) {
                        continue;
                    }
                    ArkTilePos tilePos = mapper.readValue(bytes, ArkTilePos.class);
                    ArkLevel level = parserService.parseLevel(tilePos, sha);
                    if (level == null) {
                        errorCount++;
                        log.info("[LEVEL]地图数据解析失败:" + file.getPath());
                        continue;
                    }
                    arkLevelRepo.save(level);
                    handleCount++;
                } catch (IOException e) {
                    errorCount++;
                    e.printStackTrace();
                }
            }
            log.info("[LEVEL]{}份地图数据更新成功", handleCount);
            if (errorCount > 0) {
                log.warn("[LEVEL]{}份地图数据更新失败", errorCount);
                return;
            }
            redisCache.setCacheLevelCommit(lastCommit);
        }
    }

    private Git getGitRepository() {
        if (Files.exists(Path.of(localRepository, ".git"))) {
            try {
                Repository repository = new FileRepositoryBuilder().setGitDir(Path.of(localRepository, ".git").toFile()).build();
                return new Git(repository);
            } catch (Exception exception) {
                exception.printStackTrace();
                log.warn("[LEVEL]地图数据pull失败");
                return null;
            }
        } else {
            try {
                //TODO:需要超时机制
                return Git.cloneRepository().setURI(gitRepository).setDirectory(new File(localRepository)).setNoCheckout(true).call();
            } catch (Exception exception) {
                exception.printStackTrace();
                log.warn("[LEVEL]地图数据拉取失败失败");
                return null;
            }
        }
    }

    private boolean gitCheckout(Git git, String lastCommit) {
        try {
            git.pull().call();
            git.checkout().setStartPoint(lastCommit).addPath(jsonPath).call();
        } catch (Exception exception) {
            exception.printStackTrace();
            log.warn("[LEVEL]地图数据checkout失败");
            return false;
        }
        return true;
    }
}
