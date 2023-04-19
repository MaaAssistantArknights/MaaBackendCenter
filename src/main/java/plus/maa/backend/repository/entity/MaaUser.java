package plus.maa.backend.repository.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import plus.maa.backend.controller.request.user.UserInfoUpdateDTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author AnselYuki
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Document("maa_user")
public class MaaUser implements Serializable {
    @Id
    private String userId;
    private String userName;
    @Indexed(unique = true)
    @NotBlank(message = "邮箱为唯一身份标识，不能为空")
    private String email;
    private String password;
    private Integer status = 0;
    private List<String> refreshJwtIds = new ArrayList<>();

    public void updateAttribute(UserInfoUpdateDTO updateDTO) {
        String userName = updateDTO.getUserName();
        if (!userName.isBlank()) {
            this.userName = userName;
        }
    }
}
