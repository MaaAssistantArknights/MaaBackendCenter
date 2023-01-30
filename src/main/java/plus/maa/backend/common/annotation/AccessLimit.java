package plus.maa.backend.common.annotation;

import java.lang.annotation.*;

/**
 * @author Baip1995
 */
@Inherited
@Documented
@Target({ElementType.FIELD, ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessLimit {
    /**
     * 指定second 时间内，API最多的请求次数
     */
    int times() default 3;

    /**
     * 指定时间second，redis数据过期时间
     */
    int second() default 10;
}
