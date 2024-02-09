package tech.powerjob.samples.anno;

import java.lang.annotation.*;

/**
 * 自定义方法注解
 * <a href="https://github.com/PowerJob/PowerJob/issues/770">自定义注解导致 @PowerJobHandler 失效</a>
 *
 * @author tjq
 * @since 2024/2/8
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ATestMethodAnnotation {
}
