package plus.maa.backend.controller.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 通过邮件修改密码发送验证码请求
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetVCodeDTO {
    /**
     * 邮箱
     */
    @Email(message = "邮箱格式错误")
    private String email;
}
