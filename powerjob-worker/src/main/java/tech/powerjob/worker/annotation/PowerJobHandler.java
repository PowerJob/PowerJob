package tech.powerjob.worker.annotation;


import java.lang.annotation.*;

/**
 * 方法级别的power-job调度
 * <a href="https://github.com/PowerJob/PowerJob/pull/610">PR#610</a>
 *
 * @author <a href="https://github.com/vannewang">vannewang</a>
 * @since 2023/4/6
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface PowerJobHandler {


    /**
     * handler name
     */
    String name();



}
