package tech.powerjob.server.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * AOP Utils
 *
 * @author tjq
 * @since 1/16/21
 */
@Slf4j
public class AOPUtils {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer DISCOVERER = new LocalVariableTableParameterNameDiscoverer();

    public static String parseRealClassName(JoinPoint joinPoint) {
        return joinPoint.getSignature().getDeclaringType().getSimpleName();
    }

    public static Method parseMethod(ProceedingJoinPoint joinPoint) {
        Signature pointSignature = joinPoint.getSignature();
        if (!(pointSignature instanceof  MethodSignature)) {
            throw new IllegalArgumentException("this annotation should be used on a method!");
        }
        MethodSignature signature = (MethodSignature) pointSignature;
        Method method = signature.getMethod();
        if (method.getDeclaringClass().isInterface()) {
            try {
                method = joinPoint.getTarget().getClass().getDeclaredMethod(pointSignature.getName(), method.getParameterTypes());
            } catch (SecurityException | NoSuchMethodException e) {
                ExceptionUtils.rethrow(e);
            }
        }
        return method;
    }

    public static <T> T parseSpEl(Method method, Object[] arguments, String spEl, Class<T> clazz, T defaultResult) {
        String[] params = DISCOVERER.getParameterNames(method);
        assert params != null;

        EvaluationContext context = new StandardEvaluationContext();
        for (int len = 0; len < params.length; len++) {
            context.setVariable(params[len], arguments[len]);
        }
        try {
            Expression expression = PARSER.parseExpression(spEl);
            return expression.getValue(context, clazz);
        } catch (Exception e) {
            log.error("[AOPUtils] parse SpEL failed for method[{}], please concat @tjq to fix the bug!", method.getName(), e);
            return defaultResult;
        }
    }
}
