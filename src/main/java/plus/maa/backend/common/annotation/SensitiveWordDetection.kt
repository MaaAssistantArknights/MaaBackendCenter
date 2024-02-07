package plus.maa.backend.common.annotation

/**
 * 敏感词检测注解 <br></br>
 * 用于方法上，标注该方法需要进行敏感词检测 <br></br>
 * 通过 SpEL 表达式获取方法参数
 *
 * @author lixuhuilll
 * Date: 2023-08-25 18:50
 */
@MustBeDocumented
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(
    AnnotationRetention.RUNTIME
)
annotation class SensitiveWordDetection(
    /**
     * SpEL 表达式
     */
    vararg val value: String = []
)
