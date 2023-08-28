package plus.maa.backend.common.annotation;

import java.lang.annotation.*;

/**
 * 敏感词检测注解 <br>
 * 用于方法上，标注该方法需要进行敏感词检测 <br>
 * 通过 SpEL 表达式获取方法参数
 *
 * @author lixuhuilll
 * Date: 2023-08-25 18:50
 */

@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SensitiveWordDetection {

    /**
     * SpEL 表达式
     */
    String[] value() default {};
}
