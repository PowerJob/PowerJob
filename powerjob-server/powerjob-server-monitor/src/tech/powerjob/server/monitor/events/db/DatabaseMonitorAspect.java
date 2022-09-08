package tech.powerjob.server.monitor.events.db;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.aop.support.AopUtils;
import org.springframework.stereotype.Component;
import tech.powerjob.server.common.utils.AOPUtils;
import tech.powerjob.server.monitor.monitors.ServerMonitor;

import javax.annotation.Resource;

/**
 * 监控切面
 *
 * @author tjq
 * @since 2022/9/6
 */
@Slf4j
@Aspect
@Component
public class DatabaseMonitorAspect {

    @Resource
    private ServerMonitor serverMonitor;

    @Around("execution(* tech.powerjob.server.persistence.remote.repository..*.*(..))")
    public Object monitorCoreDB(ProceedingJoinPoint joinPoint) throws Throwable {
        return wrapperMonitor(joinPoint, DatabaseType.CORE);
    }

    @Around("execution(* tech.powerjob.server.persistence.local..*.*(..))")
    public Object monitorLocalDB(ProceedingJoinPoint joinPoint) throws Throwable {
        return wrapperMonitor(joinPoint, DatabaseType.LOCAL);
    }

    private Object wrapperMonitor(ProceedingJoinPoint point, DatabaseType type) throws Throwable {
        String classNameMini = AOPUtils.parseRealClassName(point);
        final String methodName = point.getSignature().getName();

        DatabaseEvent event = new DatabaseEvent().setType(type)
                .setServiceName(classNameMini)
                .setMethodName(methodName)
                .setStatus(DatabaseEvent.Status.SUCCESS);

        long startTs = System.currentTimeMillis();
        try {
            final Object ret = point.proceed();
            event.setRows(parseEffectRows(ret));
            return ret;
        } catch (Throwable t) {
            event.setErrorMsg(t.getMessage()).setStatus(DatabaseEvent.Status.FAILED);
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - startTs;
            serverMonitor.record(event.setCost(cost));
        }
    }

    private static Integer parseEffectRows(Object ret) {
        // TODO: 计算影响行数，可能需要小改下 DAO 层，
        return null;
    }
}