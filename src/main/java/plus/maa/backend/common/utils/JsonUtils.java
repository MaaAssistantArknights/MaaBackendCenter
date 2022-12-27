package plus.maa.backend.common.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Json工具类
 *
 * @author cbc
 * @date 2022/12/26 18:22:03
 */

@Component
public class JsonUtils {

    /**
     * 缺省 ObjectMapper
     *
     * @return ObjectMapper
     */
    @Bean
    public ObjectMapper defaultObjectMapper() {
        return JsonMapper.builder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .build()
                .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    }

}