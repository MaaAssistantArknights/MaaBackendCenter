package plus.maa.backend.controller.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * @author dragove
 * created on 2023/1/19
 */
@Data
public class EmailActivateReq {
    @NotBlank(message = "激活标识符不能为空")
    private String nonce;
}
