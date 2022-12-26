package plus.maa.backend.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.beans.BeanUtils;
import plus.maa.backend.repository.entity.MaaUser;

/**
 * @author AnselYuki
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MaaUserInfo {
    private String userId;
    private String userName;
    private String activated;
    private String uploadCount;

    public MaaUserInfo(MaaUser save) {
        BeanUtils.copyProperties(save, this);
    }
}
