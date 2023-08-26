package plus.maa.backend.common.aop;

import cn.hutool.dfa.WordTree;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.jetbrains.annotations.Nullable;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import plus.maa.backend.common.annotation.SensitiveWordDetection;
import plus.maa.backend.controller.response.MaaResultException;

import java.util.List;

/**
 * 敏感词处理程序 <br>
 *
 * @author lixuhuilll
 * Date: 2023-08-25 18:50
 */

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class SensitiveWordAop {

    // 敏感词库
    private final WordTree wordTree;

    private final ObjectMapper objectMapper;

    // SpEL 表达式解析器
    private final SpelExpressionParser parser = new SpelExpressionParser();

    // 用于获取方法参数名
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Nullable
    public Object getObjectBySpEL(String spELString, JoinPoint joinPoint) {
        // 获取被注解方法
        Signature signature = joinPoint.getSignature();
        if (!(signature instanceof MethodSignature methodSignature)) {
            return null;
        }
        // 获取方法参数名数组
        String[] paramNames = nameDiscoverer.getParameterNames(methodSignature.getMethod());
        // 解析 Spring 表达式对象
        Expression expression = parser.parseExpression(spELString);
        // Spring 表达式上下文对象
        EvaluationContext context = new StandardEvaluationContext();
        // 通过 joinPoint 获取被注解方法的参数
        Object[] args = joinPoint.getArgs();
        // 给上下文赋值
        for (int i = 0; i < args.length; i++) {
            if (paramNames != null) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        context.setVariable("objectMapper", objectMapper);
        // 表达式从上下文中计算出实际参数值
        return expression.getValue(context);
    }

    @Before("@annotation(annotation)")  // 处理 SensitiveWordDetection 注解
    public void before(JoinPoint joinPoint, SensitiveWordDetection annotation) {
        // 获取 SpEL 表达式
        String[] expressions = annotation.value();
        for (String expression : expressions) {
            // 解析 SpEL 表达式
            Object value = getObjectBySpEL(expression, joinPoint);
            // 校验
            if (value instanceof String text) {
                List<String> matchAll = wordTree.matchAll(text);
                if (matchAll != null && !matchAll.isEmpty()) {
                    throw new MaaResultException(HttpStatus.BAD_REQUEST.value(), "包含敏感词：" + matchAll);
                }
            }
        }
    }
}
