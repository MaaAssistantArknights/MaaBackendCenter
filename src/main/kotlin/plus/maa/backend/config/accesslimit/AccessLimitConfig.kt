package plus.maa.backend.config.accesslimit

import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import plus.maa.backend.service.DataTransferService

@Configuration
class AccessLimitConfig(
    private val stringRedisTemplate: StringRedisTemplate,
    private val dataTransferService: DataTransferService,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(AccessLimitInterceptor(stringRedisTemplate, dataTransferService))
    }
}
