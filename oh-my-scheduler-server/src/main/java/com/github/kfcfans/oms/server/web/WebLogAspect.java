package com.github.kfcfans.oms.server.web;

import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
    @Pointcut("execution(public * com.github.kfcfans.oms.server.web.controller..*.*(..))")
    public void include() {
    }

    @Pointcut("execution(public * com.github.kfcfans.oms.server.web.controller.ServerController.*(..))")
    public void exclude() {
    }

    @Pointcut("include() && !exclude()")
    public void webLog() {
    }

    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) {
        // 获取请求域
        ServletRequestAttributes requestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return;
        }
        HttpServletRequest request = requestAttributes.getRequest();

        String[] classNameSplit = joinPoint.getSignature().getDeclaringTypeName().split("\\.");
        String classNameMini = classNameSplit[classNameSplit.length - 1];
        String classMethod = classNameMini + "." + joinPoint.getSignature().getName();

        // 排除特殊类

        // 192.168.1.1|POST|com.xxx.xxx.save|请求参数
        log.info("{}|{}|{}|{}", request.getRemoteAddr(), request.getMethod(), classMethod, JSONObject.toJSONString(joinPoint.getArgs()));
    }
}
