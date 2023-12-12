package plus.maa.backend.controller.response.copilot;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @author john180
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArkLevelInfo implements Serializable {
    private String levelId;
    private String stageId;
    private String catOne;
    private String catTwo;
    private String catThree;
    private String name;
    private int width;
    private int height;
    // 只是服务器认为的当前版本地图是否开放
    @Nullable
    @JsonIgnore
    private Boolean isOpen;
    // 非实际意义上的活动地图关闭时间，只是服务器认为的关闭时间
    @Nullable
    @JsonIgnore
    private LocalDateTime closeTime;
}
