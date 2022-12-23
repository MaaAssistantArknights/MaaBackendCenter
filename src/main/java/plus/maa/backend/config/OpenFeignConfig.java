package plus.maa.backend.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import feign.Feign;
import feign.http2client.Http2Client;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import plus.maa.backend.repository.GithubRepository;

/**
 * @author dragove
 * created on 2022/12/23
 */
@SpringBootConfiguration
public class OpenFeignConfig {

    @Bean
    public GithubRepository githubRepository() {
        ObjectMapper objectMapper = new ObjectMapper();
        // 禁用遇到未知属性抛出异常
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        return Feign.builder()
                .client(new Http2Client())
                .encoder(new JacksonEncoder(objectMapper))
                .decoder(new JacksonDecoder(objectMapper))
                .target(GithubRepository.class, "https://api.github.com");
    }

}
