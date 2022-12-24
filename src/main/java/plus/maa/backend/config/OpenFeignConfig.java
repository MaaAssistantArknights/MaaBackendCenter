package plus.maa.backend.config;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(basePackages = {
        "plus.maa.backend.repository"
})
@SpringBootConfiguration
public class OpenFeignConfig {

}
