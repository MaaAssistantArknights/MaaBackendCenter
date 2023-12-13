package plus.maa.backend.config.external;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

@lombok.Data
public class Info {
    private String title;
    private String description;
    private String version;
    private String domain;
    private String frontendDomain;
    @NestedConfigurationProperty
    private Commit commit;
}