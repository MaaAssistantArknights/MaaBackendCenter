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
public class UserInfoUpdateDTO {
    @NotBlank(message = "用户名长度应在4-24位之间")
    @Length(min = 4, max = 24, message = "用户名长度应在4-24位之间")
    private String userName;
}
