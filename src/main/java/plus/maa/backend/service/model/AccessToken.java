package plus.maa.backend.service.model;

import lombok.Data;

/**
 * @author LoMu
 * Date  2023-06-30 5:51
 */
@Data
public class AccessToken {
    private String accessToken;
    private String scope;
    private String tokenType;
}
