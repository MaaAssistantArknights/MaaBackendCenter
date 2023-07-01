package plus.maa.backend.config.external;

import lombok.Data;

/**
 * @author LoMu
 * Date  2023-06-30 5:23
 */
@Data
public class Oauth2 {
    private String clientId;
    private String clientSecret;
    private String authorize;
    private String accessToken;
    private String userInfo;
}
