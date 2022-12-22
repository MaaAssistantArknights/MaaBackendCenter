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
        return new OpenAPI()
                .info(docInfos())
                .externalDocs(new ExternalDocumentation()
                        .description("GitHub repo")
                        .url("https://github.com/MaaAssistantArknights/MaaBackendCenter"));
    }

    private Info docInfos() {
        return new Info()
                .title("MAA Copilot Center API")
                .description("MAA Copilot Backend Center")
                .version("v1.0.0")
                .license(new License()
                        .name("GNU Affero General Public License v3.0")
                        .url("https://www.gnu.org/licenses/agpl-3.0.html"));
    }
}
