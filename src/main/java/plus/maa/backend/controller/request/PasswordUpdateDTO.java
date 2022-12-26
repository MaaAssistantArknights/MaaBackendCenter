package plus.maa.backend.controller.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author AnselYuki
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class PasswordUpdateDTO {
    @NotBlank(message = "请输入原密码")
    private String originalPassword;
    @NotBlank(message = "请输入新密码")
    private String newPassword;
}
