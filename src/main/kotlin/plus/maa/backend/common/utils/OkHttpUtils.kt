package plus.maa.backend.common.utils

import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

/**
 * @author john180
 */
@Component
class OkHttpUtils {
    /**
     * 缺省 OkHttpClient
     *
     * @return OkHttpClient
     */
    @Bean
    fun defaultOkHttpClient(): OkHttpClient = OkHttpClient()
        .newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(10, 5, TimeUnit.MINUTES))
        .build()
}
