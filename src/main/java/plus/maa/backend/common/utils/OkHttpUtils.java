package plus.maa.backend.common.utils;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author john180
 */
@Component
public class OkHttpUtils {
    /**
     * 缺省 OkHttpClient
     *
     * @return OkHttpClient
     */
    @Bean
    public OkHttpClient defaultOkHttpClient() {
        return new OkHttpClient().newBuilder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .connectionPool(new ConnectionPool(10, 5, TimeUnit.MINUTES))
                .build();
    }
}
