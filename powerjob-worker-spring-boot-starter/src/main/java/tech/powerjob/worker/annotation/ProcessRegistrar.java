package tech.powerjob.worker.annotation;

import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author minsin/mintonzhang@163.com
 * @since 2024/1/16
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface ProcessRegistrar {


    /**
     * 唯一标识
     */
    String uniqueTag();

    /**
     * 任务名称
     */
    String name();

    /**
     * 任务描述
     */
    String description() default "";


    /**
     * 执行方式
     */
    ExecuteType executeType() default ExecuteType.STANDALONE;

    /**
     * 执行类型
     */
    ProcessorType processorType() default ProcessorType.BUILT_IN;


    /**
     * 定时类型
     * CRON 填写 CRON 表达式，秒级任务填写整数，API 无需填写
     */
    TimeExpressionType timeExpressionType();

    /**
     * 定时策略
     * CRON 填写 CRON 表达式，秒级任务填写整数，API 无需填写
     */
    String timeExpression();

    /**
     * 注册后自动执行
     */
    boolean enableAfterRegister() default true;
}
