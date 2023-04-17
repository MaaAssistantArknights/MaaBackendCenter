package plus.maa.backend.controller.request.file;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.Length;

import java.util.List;


/**
 * @author LoMu
 * Date  2023-04-16 17:41
 */

@Data
public class ImageDownloadDTO {
    @NotNull
    @Length(min = 1, max = 188, message = "超出长度188")
    private String type;
    private String classification;
    private List<String> version;
    private String label;
    private boolean delete;
}
