package plus.maa.backend.filter;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import plus.maa.backend.common.utils.IpUtils;
import plus.maa.backend.controller.response.MaaResult;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @author Neo.Zzj
 * @date 2023/1/20
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RegisterLimiter {

    @Value("#{'${maa-copilot.register.limiter.white-list:127.0.0.1}'.split(',')}")
    private Set<String> whiteList;
    @Value("${maa-copilot.register.limiter.enable:true}")
    private Boolean enable;
    @Value("${maa-copilot.register.limiter.time:45}")
    private Integer limitTime;

    private final HttpServletRequest httpServerRequest;

    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_PREFIX = "maa:limiter:register:";

    @Around("execution(* plus.maa.backend.service.UserService.register(..))")
    public Object countLimiter(ProceedingJoinPoint joinPoint) throws Throwable {
        if (enable == null || !enable) {
            return commonExecute(joinPoint);
        }
        String ip = IpUtils.getIpAddr(httpServerRequest);
        if (StringUtils.isEmpty(ip) || whiteList.contains(ip)) {
            return commonExecute(joinPoint);
        }
        final String redisKey = REDIS_PREFIX + ip;
        String val = redisTemplate.opsForValue().get(redisKey);

        if (val != null) {
            return MaaResult.fail(403, "请勿频繁注册!");
        }
        // 请求成功才将ip set进redis
        Object result = commonExecute(joinPoint);
        if (result instanceof MaaResult<?> && ((MaaResult<?>) result).statusCode() == MaaResult.success().statusCode()) {
            redisTemplate.opsForValue().set(redisKey, "1", limitTime, TimeUnit.MINUTES);
        }
        return result;
    }

    private Object commonExecute(ProceedingJoinPoint joinPoint) throws Throwable {
        return joinPoint.proceed();
    }
}
