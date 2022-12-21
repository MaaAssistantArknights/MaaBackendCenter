package plus.maa.backend.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * @author AnselYuki
 */
@SpringBootConfiguration
public class SpringDocConfig {
    @Bean
    public OpenAPI emergencyLogistics() {
        return new OpenAPI().info(docInfos()).externalDocs(new ExternalDocumentation().description("AnselYuki 的博客园").url("https://www.anselyuki.cn"));
    }

    private Info docInfos() {
        final String systemDescription = "无状态登录认证实验API";
        Info info = new Info();
        info.title("Spring Security Test API");
        info.description(systemDescription);
        info.version("v1.0.0");
        info.license(new License().name("本项目采用 MIT License").url("https://mit-license.org/"));
        return info;
    }
}
