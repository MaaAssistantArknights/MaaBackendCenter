package plus.maa.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import plus.maa.backend.filter.JwtAuthenticationTokenFilter;
import plus.maa.backend.handler.AccessDeniedHandlerImpl;
import plus.maa.backend.handler.AuthenticationEntryPointImpl;

/**
 * @author AnselYuki
 */
@SpringBootConfiguration
@RequiredArgsConstructor
public class SecurityConfig {
    /**
     * 添加放行接口在此处
     */
    private static final String[] URL_WHITELIST = {
            "/user/login",
            "/user/register",
            "/arknights/level",
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/test/**",
            "/version",
            "/"
    };

    private static final String[] URL_PERMIT_ALL = {
            "/user/password/reset_request",
            "/user/password/reset",
            "/copilot/query",
            "/copilot/get/**"
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
        http.authorizeHttpRequests(authorize -> {
            try {
                authorize.requestMatchers(URL_WHITELIST).anonymous()
                        .requestMatchers(URL_PERMIT_ALL).permitAll()
                        .anyRequest().authenticated();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        //添加过滤器
        http.addFilterBefore(jwtAuthenticationTokenFilter, UsernamePasswordAuthenticationFilter.class);

        //配置异常处理器，处理认证失败的JSON响应
        http.exceptionHandling().authenticationEntryPoint(authenticationEntryPoint).accessDeniedHandler(accessDeniedHandler);

        //开启跨域请求
        http.cors();
        return http.build();
    }
}
