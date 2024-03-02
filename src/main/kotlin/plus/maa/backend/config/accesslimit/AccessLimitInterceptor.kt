package plus.maa.backend.config.accesslimit

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import plus.maa.backend.common.utils.IpUtil
import plus.maa.backend.common.utils.WebUtils
import plus.maa.backend.controller.response.MaaResult.Companion.fail
import java.util.concurrent.TimeUnit

/**
 * @author Baip1995
 */
class AccessLimitInterceptor(
    private val stringRedisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
) : HandlerInterceptor {

    private val log = KotlinLogging.logger { }

    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val ann = (handler as? HandlerMethod)?.method?.getAnnotation(AccessLimit::class.java) ?: return true
        // 拼接 redis key = IP + Api 限流
        val key = IpUtil.getIpAddr(request) + request.requestURI

        // 获取 redis 的 value
        val count = stringRedisTemplate.opsForValue()[key]?.toInt() ?: 0
        if (count < ann.times) {
            // 如果 redis 中的时间比注解上的时间小则表示可以允许访问,这时修改 redis 的 value 时间
            stringRedisTemplate.opsForValue().set(
                key,
                (count + 1).toString(),
                ann.second.toLong(),
                TimeUnit.SECONDS
            )
        } else {
            // 请求过于频繁
            log.info { "$key 请求过于频繁" }
            val result = fail(HttpStatus.TOO_MANY_REQUESTS.value(), "请求过于频繁")
            val json = objectMapper.writeValueAsString(result)
            WebUtils.renderString(response, json, HttpStatus.TOO_MANY_REQUESTS.value())
            return false
        }

        return true
    }
}
