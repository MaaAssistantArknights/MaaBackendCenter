package plus.maa.backend.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 使用本储存库储存 OAuth2AuthorizationRequest 时，前端的回调请求必须携带流水号
 * 流水号必须在 HTTP_HEAD_NAME 所指示的 Http Head 中，否则将提示 [authorization_request_not_found]
 *
 * @author lixuhuilll
 * Date 2023/9/22
 */

@Component
public class RedisOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String REDIS_KEY_PREFIX = "oidc:serial:";
    @Getter
    private static final String REQUEST_KEY = "oidc_serial";
    private static final String HTTP_HEAD_NAME = "OIDC-Serial";
    // 默认缓存 20 分钟，超过 20 分钟后，授权必然失败，用户需要在 20 分钟内从 Maa Account 回调回来
    private static final int TIMEOUT = 60 * 20;

    // 不使用 RedisCache 是因为 Jackson 默认无法反序列化 OAuth2AuthorizationRequest
    private final RedisTemplate<String, OAuth2AuthorizationRequest> redisTemplate;

    public RedisOAuth2AuthorizationRequestRepository(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, OAuth2AuthorizationRequest> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // Key 使用字符串类型的序列化程序，其他则使用 Java 序列化程序
        redisTemplate.setDefaultSerializer(RedisSerializer.java());
        redisTemplate.setKeySerializer(RedisSerializer.string());
        // 手动初始化
        redisTemplate.afterPropertiesSet();
        this.redisTemplate = redisTemplate;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        String stateParameter = getStateParameter(request);
        if (stateParameter == null) {
            return null;
        }
        OAuth2AuthorizationRequest authorizationRequest = getAuthorizationRequest(request);
        return (authorizationRequest != null && stateParameter.equals(authorizationRequest.getState()))
                ? authorizationRequest : null;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }
        String state = authorizationRequest.getState();
        Assert.hasText(state, "authorizationRequest.state cannot be empty");
        String serial = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_KEY, serial);
        redisTemplate.opsForValue().set(REDIS_KEY_PREFIX + serial, authorizationRequest, TIMEOUT, TimeUnit.SECONDS);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(response, "response cannot be null");
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            redisTemplate.delete(REDIS_KEY_PREFIX + getSerial(request));
        }
        return authorizationRequest;
    }

    private String getStateParameter(HttpServletRequest request) {
        return request.getParameter(OAuth2ParameterNames.STATE);
    }

    private String getSerial(HttpServletRequest request) {
        String serial = (String) request.getAttribute(REQUEST_KEY);
        if (serial == null) {
            serial = request.getHeader(HTTP_HEAD_NAME);
        }
        return serial;
    }

    private OAuth2AuthorizationRequest getAuthorizationRequest(HttpServletRequest request) {
        String serial = getSerial(request);
        return redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + serial);
    }
}
