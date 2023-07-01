package plus.maa.backend.config.security;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import plus.maa.backend.service.model.AccessToken;

/**
 * @author LoMu
 * Date  2023-07-01 9:23
 */
@FeignClient(value = "accessToken", url = "https://github.com")
public interface Oauth2AccessToken {

    @PostMapping("/login/oauth/access_token")
    AccessToken accessToken(
            @RequestParam("client_id") String clientId,
            @RequestParam("client_secret") String clientSecret,
            @RequestParam("code") String code
    );
}
