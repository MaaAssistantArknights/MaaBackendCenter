package plus.maa.backend.config.external;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("maa-copilot")
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
    @NestedConfigurationProperty
    private SensitiveWord sensitiveWord;
    @NestedConfigurationProperty
    private Copilot copilot = new Copilot();
}
