package plus.maa.backend.config.external;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("maa-copilot")
@lombok.Data
public class MaaCopilotProperties {
    private Jwt jwt;
    private Github github;
    private Info info;
    private Vcode vcode;
    private Cache cache;
    private ArkLevelGit arkLevelGit;
    private TaskCron taskCron;
    private CopilotBackup backup;
    private Mail mail;
    private Oauth2 oauth2;
}
