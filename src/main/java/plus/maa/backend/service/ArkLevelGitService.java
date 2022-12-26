package plus.maa.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import plus.maa.backend.common.utils.converter.ArkLevelConverter;
import plus.maa.backend.controller.response.ArkLevelInfo;
import plus.maa.backend.repository.ArkLevelRepository;
import plus.maa.backend.repository.RedisCache;
import plus.maa.backend.repository.entity.ArkLevel;
import plus.maa.backend.repository.entity.ArkLevelSha;
import plus.maa.backend.repository.entity.ArknightsTilePos;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author dragove
 * created on 2022/12/23
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArkLevelGitService {
    @Value("${maa-copilot.ark-level-git.repository:}")
    private String repository;
    @Value("${maa-copilot.ark-level-git.local-repository:}")
    private String localRepository;
    @Value("${maa-copilot.ark-level-git.json-path:}")
    private String jsonPath;
    private final RedisCache redisCache;
    private final ArkLevelRepository arkLevelRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public List<ArkLevelInfo> getArkLevelInfos() {
        return arkLevelRepo.findAll()
                .stream()
                .map(ArkLevelConverter.INSTANCE::convert)
                .collect(Collectors.toList());
    }

    /**
     * 地图数据更新任务
     * 调用：1.第一次运行时或本地仓库不存在时，2.定时运行，3.github webhook
     */
    @Async
    public void runSyncLevelDataTask() {
        log.info("[LEVEL]开始同步地图数据");
        Git git = getRepository();
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
        if (!checkout(git, lastCommit)) {
            return;
        }

        File file = new File(Path.of(localRepository, jsonPath).toUri());
        if (!file.exists()) {
            log.warn("[LEVEL]地图数据获取失败, 未找到文件夹{}", jsonPath);
            return;
        }
        List<File> jsons = Arrays.stream(Objects.requireNonNull(file.listFiles())).filter(f -> f.isFile() && f.getName().endsWith(".json")).collect(Collectors.toList());
        log.info("[LEVEL]已发现{}份地图数据", jsons.size());

        List<String> shaList = arkLevelRepo.findAllShaBy().stream().map(ArkLevelSha::getSha).toList();
        ObjectInserter inserter = git.getRepository().newObjectInserter();

        int errorCount = 0;
        int handleCount = 0;
        for (File f : jsons) {
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String sha = inserter.idFor(Constants.OBJ_BLOB, bytes).name();
                if (shaList.contains(sha)) {
                    return;
                }
                ArknightsTilePos tilePos = mapper.readValue(bytes, ArknightsTilePos.class);
                ArkLevel level = parseLevel(tilePos, sha);
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

    private Git getRepository() {
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
                return Git.cloneRepository().setURI(repository).setDirectory(new File(localRepository)).setNoCheckout(true).call();
            } catch (Exception exception) {
                exception.printStackTrace();
                log.warn("[LEVEL]地图数据拉取失败失败");
                return null;
            }
        }
    }

    private boolean checkout(Git git, String lastCommit) {
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
}
