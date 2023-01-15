package tech.powerjob.server.persistence.monitor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;
import tech.powerjob.server.common.utils.AOPUtils;
import tech.powerjob.server.monitor.MonitorService;
import tech.powerjob.server.monitor.events.db.DatabaseEvent;
import tech.powerjob.server.monitor.events.db.DatabaseType;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * 监控切面
 *
 * @author tjq
 * @since 2022/9/6
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class DatabaseMonitorAspect {

    private final MonitorService monitorService;

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
            event.setErrorMsg(t.getClass().getSimpleName()).setStatus(DatabaseEvent.Status.FAILED);
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - startTs;
            monitorService.monitor(event.setCost(cost));
        }
    }

    private static Integer parseEffectRows(Object ret) {

        // 从性能角度考虑，最高频场景放在最前面判断

        if (ret instanceof Number) {
            return ((Number) ret).intValue();
        }
        if (ret instanceof Optional) {
            return ((Optional<?>) ret).isPresent() ? 1 : 0;
        }
        if (ret instanceof Collection) {
            return ((Collection<?>) ret).size();
        }
        if (ret instanceof Slice) {
            return ((Slice<?>) ret).getSize();
        }

        if (ret instanceof Stream) {
            return null;
        }
        // TODO: 直接返回对象的方法全部改成 Optional

        return ret == null ? 0 : 1;
    }
}
