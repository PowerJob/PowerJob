package tech.powerjob.server.core.lock;

import com.alibaba.fastjson.JSON;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import tech.powerjob.server.common.utils.AOPUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * aspect for @UseSegmentLock
 *
 * @author tjq
 * @since 1/16/21
 */
@Slf4j
@Aspect
@Component
@Order(1)
public class UseCacheLockAspect {

    private final Map<String, Cache<String, ReentrantLock>> lockContainer = Maps.newConcurrentMap();

    private static final long SLOW_THRESHOLD = 100;

    @Around(value = "@annotation(useCacheLock))")
    public Object execute(ProceedingJoinPoint point, UseCacheLock useCacheLock) throws Throwable {
        Cache<String, ReentrantLock> lockCache = lockContainer.computeIfAbsent(useCacheLock.type(), ignore -> {
            int concurrencyLevel = useCacheLock.concurrencyLevel();
            log.info("[UseSegmentLockAspect] create Lock Cache for [{}] with concurrencyLevel: {}", useCacheLock.type(), concurrencyLevel);
            return CacheBuilder.newBuilder()
                    .initialCapacity(300000)
                    .maximumSize(500000)
                    .concurrencyLevel(concurrencyLevel)
                    .expireAfterWrite(30, TimeUnit.MINUTES)
                    .build();
        });
        final Method method = AOPUtils.parseMethod(point);
        Long key = AOPUtils.parseSpEl(method, point.getArgs(), useCacheLock.key(), Long.class, 1L);
        final ReentrantLock reentrantLock = lockCache.get(String.valueOf(key), ReentrantLock::new);
        long start = System.currentTimeMillis();
        reentrantLock.lockInterruptibly();
        try {
            long timeCost = System.currentTimeMillis() - start;
            if (timeCost > SLOW_THRESHOLD) {
                log.warn("[UseSegmentLockAspect] wait lock for method({}#{}) cost {} ms! key = '{}', args = {}, ", method.getDeclaringClass().getSimpleName(), method.getName(), timeCost,
                        useCacheLock.key(),
                        JSON.toJSONString(point.getArgs()));
            }
            return point.proceed();
        } finally {
            reentrantLock.unlock();
        }
    }
}
