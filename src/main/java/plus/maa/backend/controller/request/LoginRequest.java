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
public class LoginRequest {
    @NotBlank(message = "请输入邮箱信息")
    private String email;
    @NotBlank(message = "请输入用户密码")
    private String password;
}
