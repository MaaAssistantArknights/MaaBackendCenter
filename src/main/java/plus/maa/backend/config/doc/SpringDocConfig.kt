package plus.maa.backend.config.doc

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springdoc.core.customizers.OperationCustomizer
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.HandlerMethod
import plus.maa.backend.common.annotation.CurrentUser

/**
 * @author AnselYuki
 */
@Configuration
class SpringDocConfig(
    @Value("\${maa-copilot.info.version}")
    private val version: String,
    @Value("\${maa-copilot.info.title}")
    private val title: String,
    @Value("\${maa-copilot.info.description}")
    private val description: String,
    @Value("\${maa-copilot.jwt.header}")
    private val securitySchemeHeader: String
) {

    @Bean
    fun emergencyLogistics(): OpenAPI {
        return OpenAPI()
            .info(docInfos())
            .externalDocs(
                ExternalDocumentation()
                    .description("GitHub repo")
                    .url("https://github.com/MaaAssistantArknights/MaaBackendCenter")
            )
            .components(
                Components()
                    .addSecuritySchemes(
                        SECURITY_SCHEME_JWT,
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .`in`(SecurityScheme.In.HEADER)
                            .name(securitySchemeHeader)
                            .description(
                                "JWT Authorization header using the Bearer scheme. Raw head example: \"$securitySchemeHeader: Bearer {token}\""
                            )
                    )
            )
    }

    /**
     * 为使用了 [CurrentUser] 注解的接口在 OpenAPI 上添加 security scheme
     */
    @Bean
    fun currentUserOperationCustomizer(): OperationCustomizer {
        return OperationCustomizer { operation: Operation, handlerMethod: HandlerMethod ->
            for (parameter in handlerMethod.methodParameters) {
                if (parameter.hasParameterAnnotation(CurrentUser::class.java)) {
                    // 已有 security scheme
                    if (operation.security.any { it.containsKey(SECURITY_SCHEME_JWT) }) {
                        break
                    }

                    // 添加 security scheme
                    operation.security.add(SecurityRequirement().addList(SECURITY_SCHEME_JWT))
                    break
                }
            }
            operation
        }
    }

    private fun docInfos() = Info()
            .title(title)
            .description(description)
            .version(version)
            .license(
                License()
                    .name("GNU Affero General Public License v3.0")
                    .url("https://www.gnu.org/licenses/agpl-3.0.html")
            )

    @Bean
    fun modelResolver(objectMapper: ObjectMapper?) = ModelResolver(objectMapper)

    companion object {
        const val SECURITY_SCHEME_JWT: String = "Jwt"
    }
}
