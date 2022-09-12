package tech.powerjob.server.web;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import tech.powerjob.server.common.utils.AOPUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

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

            // 排除特殊类

            // 192.168.1.1|POST|com.xxx.xxx.save|请求参数
            log.info("{}|{}|{}|{}", request.getRemoteAddr(), request.getMethod(), classMethod, stringify(joinPoint.getArgs()));
        }catch (Exception e) {
            // just for safe
            log.error("[WebLogAspect] aop occur exception, please concat @KFCFans to fix the bug!", e);
        }
    }

    /**
     * 序列化请求对象，需要特殊处理无法序列化的对象（HttpServletRequest/HttpServletResponse）
     * @param args Web请求参数
     * @return JSON字符串
     */
    private static String stringify(Object[] args) {
        if (ArrayUtils.isEmpty(args)) {
            return null;
        }
        List<Object> objList = Lists.newLinkedList();
        for (Object obj : args) {
            if (obj instanceof HttpServletRequest || obj instanceof HttpServletResponse) {
                break;
            }
            // FatJar
            if (obj instanceof MultipartFile || obj instanceof Resource) {
                break;
            }

            objList.add(obj);
        }
        return JSONObject.toJSONString(objList);
    }
}
