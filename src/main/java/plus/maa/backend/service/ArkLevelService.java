package plus.maa.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import plus.maa.backend.controller.response.ArkLevelInfo;
import plus.maa.backend.repository.GithubRepository;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.ArkLevelRepository;
import plus.maa.backend.repository.entity.ArkLevelSha;
import plus.maa.backend.repository.entity.ArknightsTilePos;
import plus.maa.backend.repository.entity.github.GithubCommit;
import plus.maa.backend.repository.entity.github.GithubTree;
import plus.maa.backend.repository.entity.github.GithubTrees;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
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
    private final GithubRepository githubRepo;
    private final RedisCache redisCache;
    private final ArkLevelRepository arkLevelRepo;
    private final ObjectMapper mapper = new ObjectMapper();
    private final OkHttpClient okHttpClient = new OkHttpClient().newBuilder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
            .build();
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

    public List<ArkLevelInfo> getArkLevelInfos() {
        return arkLevelRepo.findAll()
                .stream()
                .map(ArkLevelInfo::new).toList();
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
                    .filter(t -> t.getPath().equals(file) && "tree".equals(t.getType()))
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
                .filter(t -> "blob".equals(t.getType()) && t.getPath().endsWith(".json"))
                .collect(Collectors.toList());
        log.info("[LEVEL]已发现{}份地图数据", levelTrees.size());

        //根据sha筛选无需更新的地图
        List<String> shaList = arkLevelRepo.findAllShaBy().stream().map(ArkLevelSha::getSha).toList();
        levelTrees.removeIf(t -> shaList.contains(t.getSha()));
        log.info("[LEVEL]{}份地图数据需要更新", levelTrees.size());

        DownloadTask task = new DownloadTask(levelTrees.size(), t -> {
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
                ArknightsTilePos tilePos = mapper.readValue(body.string(), ArknightsTilePos.class);

                ArkLevel level = parseLevel(tilePos, tree.getSha());
                arkLevelRepo.save(level);

                task.success();
                log.info("[LEVEL]下载地图数据 {} 成功, 进度{}/{}, 用时:{}s", tilePos.getName(), task.getCurrent(), task.getTotal(), task.getDuration());
            }
        });
    }

    /**
     * 具体地图信息生成规则见
     * <a href="https://github.com/MaaAssistantArknights/MaaCopilotServer/blob/main/src/MaaCopilotServer.GameData/GameDataParser.cs">GameDataParser</a>
     * 尚未全部实现 <br>
     * TODO 完成剩余字段实现
     */
    private ArkLevel parseLevel(ArknightsTilePos tilePos, String sha) {
        String type = parseTypeName(tilePos.getLevelId());
        return ArkLevel.builder()
                .levelId(tilePos.getLevelId())
                .sha(sha)
                .catOne(type)
                .catTwo("")
                .catThree(tilePos.getStageId())
                .name(tilePos.getName())
                .width(tilePos.getWidth())
                .height(tilePos.getHeight())
                .build();
    }

    private String parseTypeName(String levelId) {
        String type;
        String[] ids = levelId.split("/");
        if (levelId.toLowerCase().startsWith("obt")) {
            type = ids[1];
        } else {
            type = ids[0];
        }
        return switch (type.toLowerCase()) {
            case "main", "hard" -> "主题曲";
            case "weekly", "promote" -> "资源收集";
            case "activities" -> "活动关卡";
            case "campaign" -> "剿灭作战";
            case "memory" -> "悖论模拟";
            case "rune" -> "危机合约";
            default -> {
                log.error("未知关卡类型:{}", levelId);
                yield "未知类型:" + type;
            }
        };
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
}
