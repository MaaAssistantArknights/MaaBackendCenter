package plus.maa.backend.controller.request.copilot;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CopilotCUDRequest {
    private String content;
    private Long id;
}
