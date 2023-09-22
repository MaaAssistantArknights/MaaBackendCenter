package plus.maa.backend.controller.response.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * OIDC 登录认证时，响应前端流水号以及重定向 URL
 *
 * @author lixuhuilll
 * Date 2023/9/22
 */

@Data
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class OIDCInfo {
    // 流水号
    private String serial;
    // 重定向 URL
    private String redirectUrl;
}
