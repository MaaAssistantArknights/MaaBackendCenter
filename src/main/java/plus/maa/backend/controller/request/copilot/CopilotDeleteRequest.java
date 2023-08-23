package plus.maa.backend.controller.request.copilot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作业删除请求
 *
 * @author lixuhuilll
 * Date 2023-08-23 17:50
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotDeleteRequest {
    private Long id;
}
