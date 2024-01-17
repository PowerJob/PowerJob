package tech.powerjob.worker.sdk;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.enums.TimeExpressionType;

/**
 * @author minsin/mintonzhang@163.com
 * @since 2024/1/16
 */

public interface ProcessRegistry {


    /**
     * 任务名称
     */
    String name();

    /**
     * 任务描述
     */
    default String description() {
        return "default";
    }


    /**
     * 执行方式
     */
    default ExecuteType executeType() {

        return ExecuteType.STANDALONE;
    }


    /**
     * 执行类型
     */
    default ProcessorType processorType() {
        return ProcessorType.BUILT_IN;
    }


    TimeExpression timeExpression();

    /**
     * 注册后自动执行
     */
    default boolean enable() {
        return true;
    }

    @Getter
    @RequiredArgsConstructor
    class TimeExpression {

        private final TimeExpressionType timeExpressionType;
        private final String timeExpression;
    }
}
