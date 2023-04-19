package plus.maa.backend.controller.response.copilot;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;

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
}
