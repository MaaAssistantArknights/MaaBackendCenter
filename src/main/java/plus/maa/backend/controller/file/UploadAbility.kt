package plus.maa.backend.controller.file;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
@Data
public class UploadAbility {
    /**
     * 是否开启上传功能
     */
    @NotNull
    Boolean enabled;
}
