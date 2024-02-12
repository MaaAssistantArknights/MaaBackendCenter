package plus.maa.backend.handler

import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus
import org.springframework.lang.Nullable
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import plus.maa.backend.common.annotation.AccessLimit
import plus.maa.backend.common.utils.IpUtil
import plus.maa.backend.common.utils.SpringUtil
import plus.maa.backend.common.utils.WebUtils
import plus.maa.backend.controller.response.MaaResult.Companion.fail
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger {  }

/**
 * @author Baip1995
 */
@Component
class AccessLimitInterceptHandlerImpl : HandlerInterceptor {
    /**
     * 接口调用前检查对方ip是否频繁调用接口
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Throws(Exception::class)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        try {
            // handler是否为 HandleMethod 实例
            if (handler is HandlerMethod) {
                // 强转
                // 获取方法
                val method = handler.method
                // 判断方式是否有AccessLimit注解，有的才需要做限流
                if (!method.isAnnotationPresent(AccessLimit::class.java)) {
                    return true
                }
                val stringRedisTemplate = SpringUtil.applicationContext!!.getBean(StringRedisTemplate::class.java)

                // 获取注解上的内容
                val accessLimit = method.getAnnotation(AccessLimit::class.java) ?: return true
                // 获取方法注解上的请求次数
                val times = accessLimit.times
                // 获取方法注解上的请求时间
                val second = accessLimit.second

                // 拼接redis key = IP + Api限流
                val key = IpUtil.getIpAddr(request) + request.requestURI

                // 获取redis的value
                var maxTimes: Int? = null

                val value = stringRedisTemplate.opsForValue()[key]
                if (!value.isNullOrEmpty()) {
                    maxTimes = value.toInt()
                }
                if (maxTimes == null) {
                    // 如果redis中没有该ip对应的时间则表示第一次调用，保存key到redis
                    stringRedisTemplate.opsForValue()[key, "1", second.toLong()] = TimeUnit.SECONDS
                } else if (maxTimes < times) {
                    // 如果redis中的时间比注解上的时间小则表示可以允许访问,这是修改redis的value时间
                    stringRedisTemplate.opsForValue()[key, (maxTimes + 1).toString() + "", second.toLong()] =
                        TimeUnit.SECONDS
                } else {
                    // 请求过于频繁
                    log.info { "$key 请求过于频繁" }
                    val result = fail(HttpStatus.TOO_MANY_REQUESTS.value(), "请求过于频繁")
                    val json = ObjectMapper().writeValueAsString(result)
                    WebUtils.renderString(response, json, HttpStatus.TOO_MANY_REQUESTS.value())
                    return false
                }
            }
        } catch (e: Exception) {
            log.error(e) { "API请求限流拦截异常，异常原因：" }
            // throw new Exception("");
        }
        return true
    }

    @Throws(Exception::class)
    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        @Nullable modelAndView: ModelAndView?
    ) {
    }

    @Throws(Exception::class)
    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        @Nullable ex: java.lang.Exception?
    ) {
    }
}
