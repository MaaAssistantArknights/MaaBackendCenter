package plus.maa.backend.service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.test.util.ReflectionTestUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.DefaultUriBuilderFactory
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import kotlin.time.Duration.Companion.minutes

@SpringBootTest
class ArkLevelServiceTest(
    @Autowired
    private val service: ArkLevelService,
    @Autowired
    private val gameDataService: ArkGameDataService,
) {

    init {
        val client = WebClient.builder()
            // your own proxy settings
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create().proxy {
                        it.type(ProxyProvider.Proxy.HTTP).host("127.0.0.1").port(10809)
                    },
                ),
            )
            .uriBuilderFactory(DefaultUriBuilderFactory().apply { encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE })
            .build()
        ReflectionTestUtils.setField(service, "webClient", client)
        ReflectionTestUtils.setField(gameDataService, "webClient", client)
    }

    @Test
    fun testSyncLevel() = runTest(timeout = 3.minutes) {
        gameDataService.syncGameData()
        service.syncLevelData()
    }

    @Test
    fun testUpdateActivitiesOpenStatus() = runTest {
        service.updateActivitiesOpenStatus()
    }

    @Test
    fun testUpdateCrisisV2OpenStatus() = runTest {
        service.updateCrisisV2OpenStatus()
    }
}
