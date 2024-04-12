package plus.maa.backend.config.accesslimit

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

@Configuration
class AccessLimitConfig(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(AccessLimitInterceptor(stringRedisTemplate, objectMapper))
    }
}
