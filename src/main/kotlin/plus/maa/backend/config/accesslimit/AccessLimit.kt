package plus.maa.backend.config.accesslimit

import java.lang.annotation.Inherited

/**
 * @author Baip1995
 */
@Inherited
@MustBeDocumented
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(
    AnnotationRetention.RUNTIME
)
annotation class AccessLimit(
    /**
     * 指定 second 时间内，API 最多的请求次数
     */
    val times: Int = 3,
    /**
     * 指定时间 second，redis 数据过期时间
     */
    val second: Int = 10
)
