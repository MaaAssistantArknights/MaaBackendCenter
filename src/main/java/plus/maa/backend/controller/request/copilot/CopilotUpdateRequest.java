package plus.maa.backend.controller.request.copilot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotUpdateRequest {
    private String content;
    private boolean hidden = false; // 是否隐藏作业
    private Long id;
}
