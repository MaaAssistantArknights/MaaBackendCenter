package plus.maa.backend.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author AnselYuki
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    /**
     * 添加放行接口在此处
     */
    private static final String[] URL_WHITELIST = {
            "/user/login",
            "/user/register",
            "/user/sendRegistrationToken"
    };

    private static final String[] URL_PERMIT_ALL = {
            "/",
            "/version",
            "/user/activateAccount",
            "/user/password/reset_request",
            "/user/password/reset",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/arknights/level",
            "/copilot/query",
            "/copilot/get/**",
            "/copilot/rating",
            "/comments/query"
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
    };
    private final AuthenticationConfiguration authenticationConfiguration;
    private final JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter;
    private final AuthenticationEntryPointImpl authenticationEntryPoint;
    private final AccessDeniedHandlerImpl accessDeniedHandler;

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
        http.csrf().disable()
                //不通过Session获取SecurityContext
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

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

        //添加过滤器
        http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        //配置异常处理器，处理认证失败的JSON响应
        http.exceptionHandling().authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler);

        //开启跨域请求
        http.cors();
        return http.build();
    }
}
