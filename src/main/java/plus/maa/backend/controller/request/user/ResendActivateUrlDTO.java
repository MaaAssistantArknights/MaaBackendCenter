package plus.maa.backend.controller.request.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendActivateUrlDTO {
    @NotBlank(message = "邮箱格式错误")
    @Email(message = "邮箱格式错误")
    private String email;
}
