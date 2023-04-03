package plus.maa.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import plus.maa.backend.common.annotation.CurrentUser;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author AnselYuki
 */
@Configuration
public class SpringDocConfig {

    @Value("${maa-copilot.info.version}")
    private String version;

    @Value("${maa-copilot.info.title}")
    private String title;

    @Value("${maa-copilot.info.description}")
    private String description;

    @Value("${maa-copilot.jwt.header}")
    private String securitySchemeHeader;

    public static final String SECURITY_SCHEME_NAME = "Bearer";

    @Bean
    public OpenAPI emergencyLogistics() {
        return new OpenAPI()
                .info(docInfos())
                .externalDocs(new ExternalDocumentation()
                        .description("GitHub repo")
                        .url("https://github.com/MaaAssistantArknights/MaaBackendCenter"))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .in(SecurityScheme.In.HEADER)
                                        .name(securitySchemeHeader)
                                        .description("JWT Authorization header using the Bearer scheme. Raw head example: \"%s: Bearer {token}\"".formatted(securitySchemeHeader))
                        ));
    }

    /**
     * 为使用了 {@link CurrentUser} 注解的接口在 OpenAPI 上添加 security scheme
     */
    @Bean
    public OperationCustomizer currentUserOperationCustomizer() {
        return (operation, handlerMethod) -> {
            for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
                if (parameter.hasParameterAnnotation(CurrentUser.class)) {
                    var security = Optional.ofNullable(operation.getSecurity());
                    // 已有 security scheme
                    if (security.stream().flatMap(List::stream).anyMatch(s -> s.containsKey(SECURITY_SCHEME_NAME))) {
                        break;
                    }

                    // 添加 security scheme
                    operation.setSecurity(security.orElseGet(ArrayList::new));
                    operation.getSecurity().add(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
                    break;
                }
            }
            return operation;
        };
    }

    private Info docInfos() {
        return new Info()
                .title(title)
                .description(description)
                .version(version)
                .license(new License()
                        .name("GNU Affero General Public License v3.0")
                        .url("https://www.gnu.org/licenses/agpl-3.0.html"));
    }

    @Bean
    public ModelResolver modelResolver(ObjectMapper objectMapper) {
        return new ModelResolver(objectMapper);
    }
}
