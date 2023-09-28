package plus.maa.backend.config.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * @author AnselYuki
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    /**
     * 添加放行接口在此处
     */
    private static final String[] URL_WHITELIST = {
            "/user/login",
            "/user/register",
            "/user/sendRegistrationToken",
            "/oidc/authorization/maa-account",
            "/oidc/callback/maa-account"
    };

    private static final String[] URL_PERMIT_ALL = {
            "/",
            "/error",
            "/version",
            "/user/activateAccount",
            "/user/password/reset_request",
            "/user/password/reset",
            "/user/refresh",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/arknights/level",
            "/copilot/query",
            "/copilot/get/**",
            "/copilot/rating",
            "/comments/query",
            "/file/upload",
            "/comments/status",
            "/copilot/status"
    };

    //添加需要权限1才能访问的接口
    private static final String[] URL_AUTHENTICATION_1 = {
            "/copilot/delete",
            "/copilot/update",
            "/copilot/upload",
            "/comments/add",
            "/comments/delete"
    };

    private static final String[] URL_AUTHENTICATION_2 = {
            "/file/download/**",
            "/file/download/",
            "/file/disable",
            "/file/enable",
            "/file/upload_ability"
    };
    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;
    private final OIDCAuthenticationSuccessHandler oidcAuthenticationSuccessHandler;
    private final OIDCRedirectStrategy oidcRedirectStrategy;
    private final RedisOAuth2AuthorizationRequestRepository redisOAuth2AuthorizationRequestRepository;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        //关闭CSRF,设置无状态连接
        http.csrf(AbstractHttpConfigurer::disable)
                //不通过Session获取SecurityContext
                .sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        //允许匿名访问的接口，如果是测试想要方便点就把这段全注释掉
        http.authorizeHttpRequests(authorize ->
                authorize.requestMatchers(URL_WHITELIST).anonymous()
                        .requestMatchers(URL_PERMIT_ALL).permitAll()
                        //权限 0 未激活 1 激活  等等.. (拥有权限1必然拥有权限0 拥有权限2必然拥有权限1、0)
                        //指定接口需要指定权限才能访问 如果不开启RBAC注释掉这一段即可
                        .requestMatchers(URL_AUTHENTICATION_1).hasAuthority("1")
                        //此处用于管理员操作接口
                        .requestMatchers(URL_AUTHENTICATION_2).hasAuthority("2")
                        .anyRequest().authenticated());


        // 存在 Maa Account 配置时，才启用 OIDC
        Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginCustomizer = null;

        try {
            // 依赖于 CGLIB 子类处理，proxyBeanMethods 需要为 true
            oauth2LoginCustomizer = oauth2LoginCustomizer();
        } catch (NoSuchBeanDefinitionException e) {
            log.info("Maa Account 配置不存在，已关闭 OIDC 认证");
        }

        if (oauth2LoginCustomizer != null) {
            http.oauth2Login(oauth2LoginCustomizer);
        }

        //添加过滤器
        http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        //配置异常处理器，处理认证失败的JSON响应
        http.exceptionHandling(exceptionHandling -> exceptionHandling.authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler));
        //开启跨域请求
        http.cors(withDefaults());
        return http.build();
    }

    @Bean
    @ConditionalOnProperty("spring.security.oauth2.client.provider.maa-account.issuer-uri")
    public Customizer<OAuth2LoginConfigurer<HttpSecurity>> oauth2LoginCustomizer() {
        return login -> {
            // 以下的链接默认值以配置文件中使用 maa-account 作为 OIDC 服务器时为例
            // Get 请求访问 "/oidc/authorization/maa-account" 将自动配置参数并跳转到 OIDC 认证页面
            login.authorizationEndpoint(
                    endpoint -> {
                        endpoint.baseUri("/oidc/authorization");
                        // 请求 OIDC 认证时不再自动重定向
                        endpoint.authorizationRedirectStrategy(oidcRedirectStrategy);
                        // 不再使用 Session 储存信息
                        endpoint.authorizationRequestRepository(redisOAuth2AuthorizationRequestRepository);
                    }
            );
            // 回调接口，默认为 "/oidc/callback/maa-account"
            login.redirectionEndpoint(
                    redirection -> redirection.baseUri("/oidc/callback/*")
            );
            // 登录异常处理器
            login.failureHandler(authenticationEntryPoint::commence);
            // 登录成功处理器
            login.successHandler(oidcAuthenticationSuccessHandler);
        };
    }
}
