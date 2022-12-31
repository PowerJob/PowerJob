package tech.powerjob.remote.framework.actor;

import java.lang.annotation.*;

/**
 * 行为处理器
 *
 * @author tjq
 * @since 2022/12/31
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface Actor {

    /**
     * root path
     * @return root path
     */
    String path();
}
