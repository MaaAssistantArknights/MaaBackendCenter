package plus.maa.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import plus.maa.backend.repository.GithubRepository;

@Configuration
public class HttpInterfaceConfig {

    @Bean
    GithubRepository githubRepository() {
        WebClient client = WebClient.builder()
                .baseUrl("https://api.github.com")
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs(codecs -> codecs
                                .defaultCodecs()
                                // 最大 20MB
                                .maxInMemorySize(20 * 1024 * 1024))
                        .build())
                .defaultHeaders(headers -> {
                    headers.add("Accept", "application/vnd.github+json");
                    headers.add("X-GitHub-Api-Version", "2022-11-28");
                })
                .build();
        return HttpServiceProxyFactory.builder(WebClientAdapter.forClient(client))
                .build()
                .createClient(GithubRepository.class);
    }

}
