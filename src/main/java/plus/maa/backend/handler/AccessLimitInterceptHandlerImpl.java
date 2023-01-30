package plus.maa.backend.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import plus.maa.backend.common.annotation.AccessLimit;
import plus.maa.backend.common.utils.IpUtil;
import plus.maa.backend.common.utils.SpringUtil;
import plus.maa.backend.common.utils.WebUtils;
import plus.maa.backend.controller.response.MaaResult;
import plus.maa.backend.repository.RedisCache;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author Baip1995
 */
@Component
public class AccessLimitInterceptHandlerImpl implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AccessLimitInterceptHandlerImpl.class);




    //@Resource
    //private RedisCache redisCache;
    /**
     * 接口调用前检查对方ip是否频繁调用接口
     *
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // handler是否为 HandleMethod 实例
            if (handler instanceof HandlerMethod) {
                // 强转
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                // 获取方法
                Method method = handlerMethod.getMethod();
                // 判断方式是否有AccessLimit注解，有的才需要做限流
                if (!method.isAnnotationPresent(AccessLimit.class)) {
                    return true;
                }
                StringRedisTemplate  stringRedisTemplate = SpringUtil.getApplicationContext().getBean(StringRedisTemplate.class);

                // 获取注解上的内容
                AccessLimit accessLimit = method.getAnnotation(AccessLimit.class);
                if (accessLimit == null) {
                    return true;
                }
                // 获取方法注解上的请求次数
                int times = accessLimit.times();
                // 获取方法注解上的请求时间
                Integer second = accessLimit.second();

                // 拼接redis key = IP + Api限流
                String key = IpUtil.getIpAddr(request) + request.getRequestURI();

                // 获取redis的value
                Integer maxTimes = null;

                String value = stringRedisTemplate.opsForValue().get(key);
                if (StringUtils.isNotEmpty(value)) {
                    maxTimes = Integer.valueOf(value);
                }
                if (maxTimes == null) {
                    // 如果redis中没有该ip对应的时间则表示第一次调用，保存key到redis
                    stringRedisTemplate.opsForValue().set(key, "1", second, TimeUnit.SECONDS);
                } else if (maxTimes < times) {
                    // 如果redis中的时间比注解上的时间小则表示可以允许访问,这是修改redis的value时间
                    stringRedisTemplate.opsForValue().set(key, maxTimes + 1 + "", second, TimeUnit.SECONDS);
                } else {
                    // 请求过于频繁
                    logger.info(key + " 请求过于频繁");
                    MaaResult<Void> result = MaaResult.fail(HttpStatus.TOO_MANY_REQUESTS.value(), "请求过于频繁");
                    String json = new ObjectMapper().writeValueAsString(result);
                    WebUtils.renderString(response, json , HttpStatus.TOO_MANY_REQUESTS.value());
                    return false;
                }
            }
        } catch (Exception e) {
            logger.error("API请求限流拦截异常，异常原因：", e);
            //throw new Exception("");
        }
        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {

    }


}
