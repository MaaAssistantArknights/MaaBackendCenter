package plus.maa.backend.controller.response;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author AnselYuki
 */
@Data
@Component
@ConfigurationProperties("maa-copilot.info")
public class MaaSystemInfo {
    private String name;
    private String version;
}
