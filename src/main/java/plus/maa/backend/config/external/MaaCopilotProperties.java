package plus.maa.backend.config.external;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@ConfigurationProperties("maa-copilot")
@lombok.Data
public class MaaCopilotProperties {
    @NestedConfigurationProperty
    private Jwt jwt;
    @NestedConfigurationProperty
    private Github github;
    @NestedConfigurationProperty
    private Info info;
    @NestedConfigurationProperty
    private Vcode vcode;
    @NestedConfigurationProperty
    private Cache cache;
    @NestedConfigurationProperty
    private ArkLevelGit arkLevelGit;
    @NestedConfigurationProperty
    private TaskCron taskCron;
    @NestedConfigurationProperty
    private CopilotBackup backup;
    @NestedConfigurationProperty
    private Mail mail;
}
