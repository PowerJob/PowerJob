package tech.powerjob.server.web;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tech.powerjob.server.common.utils.AOPUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * 使用AOP记录访问日志
 *
 * @author tjq
 * @since 2020/6/5
 */
@Aspect
@Component
@Slf4j(topic = "WEB_LOG")
public class WebLogAspect {

    /**
     * 定义切入点
     * 第一个*：标识所有返回类型
     * 字母路径：包路径
     * 两个点..：当前包以及子包
     * 第二个*：所有的类
     * 第三个*：所有的方法
     * 最后的两个点：所有类型的参数
     */
    @Pointcut("execution(public * tech.powerjob.server.web.controller..*.*(..))")
    public void include() {
    }

    @Pointcut("execution(public * tech.powerjob.server.web.controller.ServerController.*(..))")
    public void exclude() {
    }

    @Pointcut("include() && !exclude()")
    public void webLog() {
    }

    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) {
        try {
            // 获取请求域
            ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (requestAttributes == null) {
                return;
            }
            HttpServletRequest request = requestAttributes.getRequest();


            String classNameMini = AOPUtils.parseRealClassName(joinPoint);
            String classMethod = classNameMini + "." + joinPoint.getSignature().getName();

            // Request bodies can contain passwords, tokens and nested third-party credentials.
            // Keep access metadata without serializing arbitrary controller arguments.
            log.info("{}|{}|{}", request.getRemoteAddr(), request.getMethod(), classMethod);
        }catch (Exception e) {
            // just for safe
            log.error("[WebLogAspect] aop occur exception, please concat @KFCFans to fix the bug!", e);
        }
    }

}
