package plus.maa.backend.service.level

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import kotlin.time.Duration.Companion.minutes

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ArkLevelServiceTest(
    @Autowired
    private val service: ArkLevelService,
) {
    /*** your own proxy settings ***/
//    init {
//        val client = WebClient.builder()
//            .clientConnector(
//                ReactorClientHttpConnector(
//                    HttpClient.create().proxy {
//                        it.type(ProxyProvider.Proxy.HTTP).host("127.0.0.1").port(10809)
//                    },
//                ),
//            )
//            .uriBuilderFactory(DefaultUriBuilderFactory().apply { encodingMode = DefaultUriBuilderFactory.EncodingMode.NONE })
//            .build()
//        ReflectionTestUtils.setField(service, "webClient", client)
//    }

    @Order(1)
    @Test
    fun testSyncLevel() = runTest(timeout = 3.minutes) {
        service.syncLevelData()
    }

    @Order(2)
    @Test
    fun testUpdateActivitiesOpenStatus() = runTest(timeout = 3.minutes) {
        service.updateActivitiesOpenStatus()
    }

    @Order(3)
    @Test
    fun testUpdateCrisisV2OpenStatus() = runTest(timeout = 3.minutes) {
        service.updateCrisisV2OpenStatus()
    }
}
