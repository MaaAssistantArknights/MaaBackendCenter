package plus.maa.backend.controller.request.user;

import jakarta.validation.constraints.Email;
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
public class RegisterDTO {
    @NotBlank(message = "邮箱格式错误")
    @Email(message = "邮箱格式错误")
    private String email;
    @NotBlank(message = "用户名长度应在4-24位之间")
    @Length(min = 4, max = 24, message = "用户名长度应在4-24位之间")
    private String userName;
    @NotBlank(message = "密码长度必须在8-32位之间")
    @Length(min = 8, max = 32, message = "密码长度必须在8-32位之间")
    private String password;
    @NotBlank(message = "请输入验证码")
    private String registrationToken;
}
