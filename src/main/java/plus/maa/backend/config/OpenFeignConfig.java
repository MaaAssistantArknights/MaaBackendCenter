package plus.maa.backend.config;

import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Configuration;

@EnableFeignClients(basePackages = {
        "plus.maa.backend.repository"
})
@Configuration
public class OpenFeignConfig {

}
