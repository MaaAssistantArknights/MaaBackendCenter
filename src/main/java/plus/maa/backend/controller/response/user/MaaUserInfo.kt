package plus.maa.backend.controller.response.user;

import org.springframework.beans.BeanUtils;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
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
    private String id;
    private String userName;
    private boolean activated;
    private long uploadCount;

    public MaaUserInfo(MaaUser save) {
        BeanUtils.copyProperties(save, this);
    }
}
