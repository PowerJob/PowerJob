package com.github.kfcfans.powerjob.server.service.lock.local;

import com.github.kfcfans.powerjob.common.utils.SegmentLock;
import com.github.kfcfans.powerjob.server.common.utils.AOPUtils;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Description
 *
 * @author tjq
 * @since 1/16/21
 */
@Slf4j
@Aspect
@Component
public class UseSegmentLockAspect {

    private final Map<String, SegmentLock> lockStore = Maps.newConcurrentMap();

    @Around(value = "@annotation(useSegmentLock))")
    public Object execute(ProceedingJoinPoint point, UseSegmentLock useSegmentLock) throws Throwable {
        SegmentLock segmentLock = lockStore.computeIfAbsent(useSegmentLock.type(), ignore -> {
            int concurrencyLevel = useSegmentLock.concurrencyLevel();
            log.info("[UseSegmentLockAspect] create SegmentLock for [{}] with concurrencyLevel: {}", useSegmentLock.type(), concurrencyLevel);
            return new SegmentLock(concurrencyLevel);
        });

        int index = AOPUtils.parseSpEl(AOPUtils.parseMethod(point), point.getArgs(), useSegmentLock.key(), Integer.class, 1);
        segmentLock.lockInterruptibleSafe(index);
        return point.proceed();
    }
}
