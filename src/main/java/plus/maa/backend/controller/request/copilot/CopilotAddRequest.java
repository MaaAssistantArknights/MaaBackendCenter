package plus.maa.backend.controller.request.copilot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作业创建请求
 *
 * @author lixuhuilll
 * Date 2023-08-23 17:50
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotAddRequest {
    private String content;
    private boolean hidden = false; // 是否隐藏作业
}
