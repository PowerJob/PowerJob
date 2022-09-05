package tech.powerjob.server.monitor.events.db;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
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

    @Around(value = "@annotation(databaseMonitor))")
    public Object execute(ProceedingJoinPoint point, DatabaseMonitor databaseMonitor) throws Throwable {

        final String className = point.getTarget().getClass().getSimpleName();
        final String methodName = point.getSignature().getName();

        DatabaseEvent event = new DatabaseEvent().setType(databaseMonitor.type())
                .setServiceName(className)
                .setMethodName(methodName)
                .setStatus(DatabaseEvent.Status.SUCCESS);

        long startTs = System.currentTimeMillis();
        try {
            final Object ret = point.proceed();
            event.setRows(parseEffectRows(ret));
            return ret;
        } catch (Throwable t) {

            long cost = System.currentTimeMillis() - startTs;
            event.setCost(cost).setErrorMsg(t.getMessage()).setStatus(DatabaseEvent.Status.FAILED);
            serverMonitor.record(event);

            throw t;
        }
    }

    private static Integer parseEffectRows(Object ret) {
        // TODO: 计算影响行数，可能需要小改下 DAO 层，
        return null;
    }
}
