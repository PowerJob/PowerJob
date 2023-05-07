package tech.powerjob.worker.annotation;


import java.lang.annotation.*;

/**
 * 方法级别的power-job调度
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface PowerJob {


    /**
     * handler name
     */
    String value();



}
