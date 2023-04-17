package plus.maa.backend.controller.request.file;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * @author LoMu
 * Date  2023-04-17 13:31
 */

@Data
public class ImageUploadDTO {
    @NotNull
    private String type;
    private String classification;
    @NotNull(message = "Maa version cannot be null")
    private String version;
    private String label;
}
