package plus.maa.backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import plus.maa.backend.common.annotation.AccessLimit;
import plus.maa.backend.common.utils.IpUtil;
import plus.maa.backend.common.utils.WebUtils;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.repository.RedisCache;

import java.lang.reflect.Method;

/**
 * @author Baip1995
 */
@Component
@RequiredArgsConstructor
public class AccessLimitInterceptHandlerImpl implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AccessLimitInterceptHandlerImpl.class);

    private final RedisCache redisCache;
    private final ObjectMapper objectMapper;

    /**
     * 接口调用前检查对方ip是否频繁调用接口
     *
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            // handler是否为 HandleMethod 实例
            if (handler instanceof HandlerMethod handlerMethod) {
                // 获取方法
                Method method = handlerMethod.getMethod();
                // 判断方式是否有AccessLimit注解，有的才需要做限流
                if (!method.isAnnotationPresent(AccessLimit.class)) {
                    return true;
                }

                // 获取注解上的内容
                AccessLimit accessLimit = method.getAnnotation(AccessLimit.class);
                if (accessLimit == null) {
                    return true;
                }
                // 获取方法注解上的请求次数
                int times = accessLimit.times();
                // 获取方法注解上的请求时间
                int second = accessLimit.second();
                // 计算 Qps
                double qps = 1.0 * times / second;

                // 拼接redis key = IP + Api限流
                String key = IpUtil.getIpAddr(request) + request.getRequestURI();

                if (!redisCache.tokenBucket(key, times, qps)) {
                    // 请求过于频繁
                    logger.info(key + " 请求过于频繁");
                    MaaResult<Void> result = MaaResult.fail(HttpStatus.TOO_MANY_REQUESTS.value(), "请求过于频繁");
                    String json = objectMapper.writeValueAsString(result);
                    WebUtils.renderString(response, json, HttpStatus.TOO_MANY_REQUESTS.value());
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("API请求限流拦截异常，异常原因：", e);
            // throw new Exception("");
        }
        return true;
    }

}
