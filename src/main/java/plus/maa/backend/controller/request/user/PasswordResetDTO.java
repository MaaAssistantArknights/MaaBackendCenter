package plus.maa.backend.controller.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 通过邮件修改密码请求
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetDTO {
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式错误")
    private String email;
    /**
     * 验证码
     */
    @NotBlank(message = "请输入验证码")
    private String activeCode;
    /**
     * 修改后的密码
     */
    @NotBlank(message = "请输入用户密码")
    private String password;
}
