package plus.maa.backend.controller.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MaaLoginRsp {
    private String token;
    private LocalDateTime validBefore;
    private LocalDateTime  validAfter;
    private String refreshToken;
    private LocalDateTime   refreshTokenValidBefore;
    private LocalDateTime    refreshTokenValidAfter;
    private MaaUserInfo userInfo;
}
