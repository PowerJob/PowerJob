package tech.powerjob.remote.framework.actor;

import java.lang.annotation.*;

/**
 * Handler
 *
 * @author tjq
 * @since 2022/12/31
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Handler {

    /**
     * handler path
     * @return handler path
     */
    String path();
}
