package plus.maa.backend.controller.request;

import jakarta.validation.constraints.Email;
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
    @Email(message = "邮箱格式错误")
    private String email;
    @Length(min = 2, max = 20, message = "用户名长度应在2-20之间")
    private String userName;
    @Length(min = 6, max = 20, message = "密码长度应在6-20之间")
    private String password;
}
