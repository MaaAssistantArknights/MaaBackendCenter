package plus.maa.backend.controller.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class MaaLoginRsp {
    private String token;
    private Date validBefore;
    private Date validAfter;
    private String refreshToken;
    private Date refreshTokenValidBefore;
    private Date refreshTokenValidAfter;
    private MaaUserInfo userInfo;
}
