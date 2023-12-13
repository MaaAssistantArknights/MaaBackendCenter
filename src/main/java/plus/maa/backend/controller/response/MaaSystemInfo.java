package plus.maa.backend.controller.response;

import lombok.Data;
import plus.maa.backend.config.external.Commit;

/**
 * @author AnselYuki
 */
@Data
public class MaaSystemInfo {
    private String title;
    private String description;
    private String version;
    private Commit commit;
}
