package plus.maa.backend.controller.request.copilot;


import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import plus.maa.backend.repository.entity.Copilot;

import java.util.List;

/**
 * @author LoMu
 * Date  2023-01-10 19:50
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotDTO {

    //关卡名
    @NotBlank(message = "关卡名不能为空")
    private String stageName;

    //难度
    private int difficulty;

    //版本号(文档中说明:最低要求 maa 版本号，必选。保留字段)
    @NotBlank(message = "最低要求 maa 版本不可为空")
    private String minimumRequired;

    //指定干员
    private List<Copilot.Operators> opers;
    //群组
    private List<Copilot.Groups> groups;
    // 战斗中的操作
    private List<Copilot.Action> actions;

    //描述
    private Copilot.Doc doc;
}
