package plus.maa.backend.controller.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author john180
 */
@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArkLevelInfo {
    private String levelId;
    private String stageId;
    private String catOne;
    private String catTwo;
    private String catThree;
    private String name;
    private int width;
    private int height;
}
