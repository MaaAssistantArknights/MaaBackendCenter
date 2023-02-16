package plus.maa.backend.task;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import plus.maa.backend.config.external.CopilotBackup;
import plus.maa.backend.config.external.MaaCopilotProperties;
import plus.maa.backend.repository.CopilotRepository;
import plus.maa.backend.repository.entity.Copilot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
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

    private Git git;

    private PersonIdent committer;

    @PostConstruct
    public void initRepo() {
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
        committer = new PersonIdent(backup.getUserName(), backup.getEmail());
        try (Stream<Path> fileList = Files.list(repoDir.toPath())) {
            if (fileList.findFirst().isEmpty()) {
                // 不存在文件则初始化
                git = Git.cloneRepository()
                        .setURI(backup.getUri())
                        .setDirectory(repoDir)
                        .call();
            } else {
                git = new Git(new FileRepositoryBuilder()
                        .setGitDir(repoDir)
                        .readEnvironment().findGitDir().build());
            }
        } catch (IOException | GitAPIException e) {
            log.error("init copilot backup repo failed, repoDir: {}", repoDir, e);
        }
    }

    /**
     * copilot数据同步定时任务，每天执行一次
     * 应用启动时自动同步一次
     */
    @Scheduled(cron = "${maa-copilot.task-cron.copilot-update:-}")
    public void backupCopilots() {
        if (config.getBackup().isDisabled()) {
            return;
        }
        if (git == null) {
            return;
        }
        try {
            git.pull().call();
        } catch (GitAPIException e) {
            log.error("git pull execute failed, msg: {}", e.getMessage(), e);
        }

        // TODO 往repo里塞入最新的copilot文件
        List<Copilot> copilots = copilotRepository.findAll();

        try {
            Status status = git.status().call();
            if (status.getAdded().isEmpty() &&
                    status.getChanged().isEmpty() &&
                    status.getRemoved().isEmpty()) {
                log.info("copilot backup with no new added or changes");
                return;
            }
            git.add().addFilepattern(".").call();
            git.commit().setCommitter(committer)
                    .setMessage("done backup date: " + LocalDate.now())
                    .call();
            git.push().call();
        } catch (GitAPIException e) {
            log.error("git committing failed, msg: {}", e.getMessage(), e);
        }
    }

}
