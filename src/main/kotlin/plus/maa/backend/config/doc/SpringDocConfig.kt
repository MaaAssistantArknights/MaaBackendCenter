package plus.maa.backend.config.doc

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.core.jackson.ModelResolver
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.ExternalDocumentation
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import plus.maa.backend.config.external.MaaCopilotProperties

/**
 * @author AnselYuki
 */
@Configuration
class SpringDocConfig(
    properties: MaaCopilotProperties,
) {
    private val info = properties.info
    private val jwt = properties.jwt

    @Bean
    fun emergencyLogistics(): OpenAPI = OpenAPI().apply {
        info(
            Info().apply {
                title(this@SpringDocConfig.info.title)
                description(this@SpringDocConfig.info.description)
                version(this@SpringDocConfig.info.version)
                license(
                    License().apply {
                        name("GNU Affero General Public License v3.0")
                        url("https://www.gnu.org/licenses/agpl-3.0.html")
                    },
                )
            },
        )
        externalDocs(
            ExternalDocumentation().apply {
                description("GitHub repo")
                url("https://github.com/ZOOT-Plus/ZootPlusBackend")
            },
        )
        components(
            Components().apply {
                addSecuritySchemes(
                    SECURITY_SCHEME_JWT,
                    SecurityScheme().apply {
                        type(SecurityScheme.Type.HTTP)
                        scheme("bearer")
                        `in`(SecurityScheme.In.HEADER)
                        name(jwt.header)
                        val s =
                            "JWT Authorization header using the Bearer scheme. Raw head example: " +
                                "\"${jwt.header}: Bearer {token}\""
                        description(s)
                    },
                )
            },
        )
        addSecurityItem(SecurityRequirement().addList(SECURITY_SCHEME_JWT))
    }

    @Bean
    fun modelResolver(objectMapper: ObjectMapper) = ModelResolver(objectMapper)

    companion object {
        const val SECURITY_SCHEME_JWT: String = "Jwt"
    }
}
