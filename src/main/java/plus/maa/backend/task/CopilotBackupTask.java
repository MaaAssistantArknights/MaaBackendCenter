package plus.maa.backend.task;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import plus.maa.backend.config.external.CopilotBackup;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.controller.response.copilot.ArkLevelInfo;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.entity.Copilot;
import plus.maa.backend.service.ArkLevelService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * CopilotBackupTask
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CopilotBackupTask {

    private final MaaCopilotProperties config;

    private final CopilotRepository copilotRepository;

    private final ArkLevelService levelService;

    private Git git;
    private static final File DEFAULT_SSH_DIR = new File(FS.DETECTED.userHome(), "/.ssh");

    private static final TransportConfigCallback sshCallback = transport -> {
        if (transport instanceof SshTransport sshTransport) {
            sshTransport.setSshSessionFactory(new SshdSessionFactoryBuilder()
                    .setPreferredAuthentications("publickey")
                    .setHomeDirectory(FS.DETECTED.userHome())
                    .setSshDirectory(DEFAULT_SSH_DIR)
                    .build(null));
        }
    };

    /**
     * 初始化Git对象，如果目录已经存在且存在文件，则直接当作git仓库，如果不存在则clone仓库
     */
    @PostConstruct
    public void initGit() {
        CopilotBackup backup = config.getBackup();
        if (backup.isDisabled()) {
            return;
        }
        File repoDir = new File(backup.getDir());
        if (repoDir.mkdirs()) {
            log.info("directory not exist, created: {}", backup.getDir());
        } else {
            log.info("directory already exists, dir: {}", backup.getDir());
        }
        if (!repoDir.isDirectory()) {
            return;
        }
        try (Stream<Path> fileList = Files.list(repoDir.toPath())) {
            if (fileList.findFirst().isEmpty()) {
                // 不存在文件则初始化
                git = Git.cloneRepository()
                        .setURI(backup.getUri())
                        .setDirectory(repoDir)
                        .setTransportConfigCallback(sshCallback)
                        .call();
            } else {
                git = Git.open(repoDir);
            }
        } catch (IOException | GitAPIException e) {
            log.error("init copilot backup repo failed, repoDir: {}", repoDir, e);
        }
    }

    /**
     * copilot数据同步定时任务，每天执行一次
     */
    @Scheduled(cron = "${maa-copilot.task-cron.copilot-update:-}")
    public void backupCopilots() {
        if (config.getBackup().isDisabled() || Objects.isNull(git)) {
            return;
        }
        try {
            git.pull().call();
        } catch (GitAPIException e) {
            log.error("git pull execute failed, msg: {}", e.getMessage(), e);
        }

        File baseDirectory = git.getRepository().getWorkTree();
        List<Copilot> copilots = copilotRepository.findAll();
        copilots.forEach(copilot -> {
            ArkLevelInfo level = levelService.findByLevelIdFuzzy(copilot.getStageName());
            if (Objects.isNull(level)) {
                return;
            }
            // 暂时使用 copilotId 作为文件名
            File filePath = new File(String.join(File.separator, baseDirectory.getPath(), level.getCatOne(),
                    level.getCatTwo(), level.getCatThree(), copilot.getCopilotId() + ".json"));
            String content = copilot.getContent();
            if (Objects.isNull(content)) {
                return;
            }
            if (copilot.isDelete()) {
                // 删除文件
                deleteCopilot(filePath);
            } else {
                // 创建或者修改文件
                upsertCopilot(filePath, content);
            }
        });

        doCommitAndPush();
    }

    private void upsertCopilot(File file, String content) {
        if (!file.exists()) {
            if (!file.getParentFile().mkdirs()) {
                log.warn("folder may exists, mkdir failed");
            }
        }
        try {
            Files.writeString(file.toPath(), content);
        } catch (IOException e) {
            log.error("write file failed, path: {}, message: {}", file.getPath(), e.getMessage(), e);
        }
    }

    private void deleteCopilot(File file) {
        if (file.exists()) {
            if (file.delete()) {
                log.info("delete copilot file: {}", file.getPath());
            } else {
                log.error("delete copilot failed, file: {}", file.getPath());
            }
        } else {
            log.info("file does not exists, no need to delete");
        }
    }

    private void doCommitAndPush() {
        try {
            Status status = git.status().call();
            if (status.getAdded().isEmpty() &&
                    status.getChanged().isEmpty() &&
                    status.getRemoved().isEmpty() &&
                    status.getUntracked().isEmpty() &&
                    status.getModified().isEmpty() &&
                    status.getAdded().isEmpty()) {
                log.info("copilot backup with no new added or changes");
                return;
            }
            git.add().addFilepattern(".").call();
            CopilotBackup backup = config.getBackup();
            PersonIdent committer = new PersonIdent(backup.getUsername(), backup.getEmail());
            git.commit().setCommitter(committer)
                    .setMessage(LocalDate.now().toString())
                    .call();
            git.push()
                    .setTransportConfigCallback(sshCallback)
                    .call();
        } catch (GitAPIException e) {
            log.error("git committing failed, msg: {}", e.getMessage(), e);
        }
    }

}
