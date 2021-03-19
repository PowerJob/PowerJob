package tech.powerjob.server.core.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * use segment lock to make concurrent safe
 *
 * @author tjq
 * @since 1/16/21
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseSegmentLock {

    String type();

    String key();

    int concurrencyLevel();
}
