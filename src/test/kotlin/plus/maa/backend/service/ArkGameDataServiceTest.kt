package plus.maa.backend.service

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.ProxyProvider
import kotlin.time.Duration.Companion.seconds

class ArkGameDataServiceTest {
    private val arkGameDataService = ArkGameDataService(
        WebClient.builder()
            // your own proxy settings
            .clientConnector(
                ReactorClientHttpConnector(
                    HttpClient.create().proxy {
                        it.type(ProxyProvider.Proxy.HTTP).host("127.0.0.1").port(10809)
                    },
                ),
            ),
    )

    @Test
    fun testSyncStage() = runTest(timeout = 30.seconds) {
        assert(arkGameDataService.syncStage())
    }

    @Test
    fun testSyncZone() = runTest(timeout = 30.seconds) {
        assert(arkGameDataService.syncZone())
    }

    @Test
    fun testSyncActivity() = runTest(timeout = 30.seconds) {
        assert(arkGameDataService.syncActivity())
    }

    @Test
    fun testSyncCharacter() = runTest(timeout = 30.seconds) {
        assert(arkGameDataService.syncCharacter())
    }

    @Test
    fun testSyncTower() = runTest(timeout = 30.seconds) {
        assert(arkGameDataService.syncTower())
    }

    @Test
    fun testSyncCrisisV2Info() = runTest(timeout = 30.seconds) {
        assert(arkGameDataService.syncCrisisV2Info())
    }
}
