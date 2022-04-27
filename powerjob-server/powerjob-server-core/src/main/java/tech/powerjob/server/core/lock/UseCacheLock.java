package tech.powerjob.server.core.lock;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * use cached lock to make concurrent safe
 *
 * @author tjq
 * @author Echo009
 * @since 1/16/21
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface UseCacheLock {

    String type();

    String key();

    int concurrencyLevel();
}
