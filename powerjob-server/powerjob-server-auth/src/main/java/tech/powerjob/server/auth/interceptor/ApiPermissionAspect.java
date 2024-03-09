package tech.powerjob.server.auth.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * ApiPermission 切面
 * 主要用于执行授权插件，完成创建后授权
 *
 * @author tjq
 * @since 2024/2/11
 */
@Slf4j
@Aspect
@Component
public class ApiPermissionAspect {

    @Pointcut("@annotation(ApiPermission)")
    public void apiPermissionPointcut() {
        // 定义切入点
    }

    /**
     * 后置返回
     *      如果第一个参数为JoinPoint，则第二个参数为返回值的信息
     *      如果第一个参数不为JoinPoint，则第一个参数为returning中对应的参数
     * returning：限定了只有目标方法返回值与通知方法参数类型匹配时才能执行后置返回通知，否则不执行，
     *            参数为Object类型将匹配任何目标返回值
     *  After注解标注的方法会在目标方法执行后运行，无论目标方法是正常完成还是抛出异常。它相当于finally块，因为它总是执行，所以适用于释放资源等清理活动。@After注解不能访问目标方法的返回值。
     *  AfterReturning注解标注的方法仅在目标方法成功执行后（即正常返回）运行。它可以访问目标方法的返回值。使用@AfterReturning可以在方法正常返回后执行一些逻辑，比如对返回值进行处理或验证。
     */
    @AfterReturning(value = "apiPermissionPointcut()", returning = "result")
    public void doAfterReturningAdvice1(JoinPoint joinPoint, Object result) {

        // 入参
        Object[] args = joinPoint.getArgs();

        // 获取目标方法
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        ApiPermission annotationAnno = AnnotationUtils.getAnnotation(method, ApiPermission.class);

        assert annotationAnno != null;

        Class<? extends GrantPermissionPlugin> grandPermissionPluginClz = annotationAnno.grandPermissionPlugin();

        try {
            GrantPermissionPlugin grandPermissionPlugin = grandPermissionPluginClz.getDeclaredConstructor().newInstance();
            grandPermissionPlugin.grant(args, result, method, joinPoint.getTarget());
        } catch (Exception e) {
            log.error("[ApiPermissionAspect] process ApiPermission grant failed", e);
            ExceptionUtils.rethrow(e);
        }
    }
}
