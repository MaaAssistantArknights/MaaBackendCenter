package plus.maa.backend.common.utils

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.core.io.buffer.DataBufferUtils
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * This extension store all content in memory as well, but not require webclient to have big buffer
 */
suspend fun WebClient.ResponseSpec.awaitString(charset: Charset = StandardCharsets.UTF_8): String {
    val flux = bodyToFlux<DataBuffer>()
    val buffer = DataBufferUtils.join(flux).awaitSingle()
    val str = buffer.toString(charset)
    DataBufferUtils.release(buffer)
    return str
}
