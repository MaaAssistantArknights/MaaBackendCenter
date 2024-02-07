package plus.maa.backend.common.annotation

/**
 * @author LoMu
 * Date  2023-01-22 17:49
 */
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
@Retention(
    AnnotationRetention.RUNTIME
)
annotation class JsonSchema
