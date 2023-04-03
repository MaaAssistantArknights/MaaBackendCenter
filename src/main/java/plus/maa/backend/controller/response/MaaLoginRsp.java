package plus.maa.backend.controller.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MaaLoginRsp {
    private String token;
    private Date validBefore;
    private Date validAfter;
    private String refreshToken;
    private String refreshTokenValidBefore;
    private MaaUserInfo userInfo;
}
