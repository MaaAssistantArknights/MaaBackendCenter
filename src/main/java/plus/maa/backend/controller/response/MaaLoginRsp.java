package plus.maa.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaaLoginRsp {
    private String token;
    private String validBefore;
    private String validAfter;
    private String refreshToken;
    private String refreshTokenValidBefore;
    private MaaUserInfo userInfo;
}
