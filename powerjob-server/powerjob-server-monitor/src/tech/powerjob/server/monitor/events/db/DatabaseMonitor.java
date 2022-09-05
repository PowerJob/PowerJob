package tech.powerjob.server.monitor.events.db;

import java.lang.annotation.*;

/**
 * 数据库监控注解
 *
 * @author tjq
 * @since 2022/9/6
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DatabaseMonitor {
    /**
     * 类型，比如 H2 / CORE / MONGO
     * @return 类型
     */
    DatabaseEventType type();
}
