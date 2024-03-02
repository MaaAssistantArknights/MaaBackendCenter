package plus.maa.backend.common.aop

import cn.hutool.dfa.WordTree
import com.fasterxml.jackson.databind.ObjectMapper
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.expression.EvaluationContext
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import plus.maa.backend.common.annotation.SensitiveWordDetection
import plus.maa.backend.controller.response.MaaResultException

/**
 * 敏感词处理程序 <br></br>
 *
 * @author lixuhuilll
 * Date: 2023-08-25 18:50
 */
@Aspect
@Component
class SensitiveWordAop(
    // 敏感词库
    private val wordTree: WordTree,
    private val objectMapper: ObjectMapper
) {

    // SpEL 表达式解析器
    private val parser = SpelExpressionParser()

    // 用于获取方法参数名
    private val nameDiscoverer = DefaultParameterNameDiscoverer()

    fun getObjectBySpEL(spELString: String, joinPoint: JoinPoint): Any? {
        // 获取被注解方法
        val signature = joinPoint.signature as? MethodSignature ?: return null
        // 获取方法参数名数组
        val paramNames = nameDiscoverer.getParameterNames(signature.method)
        // 解析 Spring 表达式对象
        val expression = parser.parseExpression(spELString)
        // Spring 表达式上下文对象
        val context: EvaluationContext = StandardEvaluationContext()
        // 通过 joinPoint 获取被注解方法的参数
        val args = joinPoint.args
        // 给上下文赋值
        for (i in args.indices) {
            if (paramNames != null) {
                context.setVariable(paramNames[i], args[i])
            }
        }
        context.setVariable("objectMapper", objectMapper)
        // 表达式从上下文中计算出实际参数值
        return expression.getValue(context)
    }

    @Before("@annotation(annotation)") // 处理 SensitiveWordDetection 注解
    fun before(joinPoint: JoinPoint, annotation: SensitiveWordDetection) {
        // 获取 SpEL 表达式
        val expressions = annotation.value
        for (expression in expressions) {
            // 解析 SpEL 表达式
            val value = getObjectBySpEL(expression, joinPoint)
            // 校验
            if (value is String) {
                val matchAll = wordTree.matchAll(value)
                if (matchAll != null && matchAll.isNotEmpty()) {
                    throw MaaResultException(HttpStatus.BAD_REQUEST.value(), "包含敏感词：$matchAll")
                }
            }
        }
    }
}
