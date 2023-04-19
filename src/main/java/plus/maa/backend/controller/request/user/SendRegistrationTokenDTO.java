package plus.maa.backend.controller.request.user;

import jakarta.validation.constraints.Email;
import lombok.Data;

@Data
public class SendRegistrationTokenDTO {
    @Email(message = "邮箱格式错误")
    private String email;
}
