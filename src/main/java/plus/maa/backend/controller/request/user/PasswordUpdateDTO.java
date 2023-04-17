package plus.maa.backend.controller.request.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.validator.constraints.Length;

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
    @Length(min = 8, max = 32, message = "密码长度必须在8-32位之间")
    private String newPassword;
}
