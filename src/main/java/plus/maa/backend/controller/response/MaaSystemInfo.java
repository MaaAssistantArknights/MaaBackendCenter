package plus.maa.backend.controller.response;

import lombok.Data;
import org.springframework.boot.info.GitProperties;

/**
 * @author AnselYuki
 */
@Data
public class MaaSystemInfo {
    private String title;
    private String description;
    private String version;
    private GitProperties git;
}
