package tech.powerjob.worker.sdk;

import tech.powerjob.common.enums.ExecuteType;
import tech.powerjob.common.enums.ProcessorType;
import tech.powerjob.common.model.LogConfig;

/**
 * @author minsin/mintonzhang@163.com
 * @since 2024/1/16
 */

public interface ProcessRegistry {


    /**
     * 任务名称
     */
    default String name() {
        return "DEFAULT";
    }

    /**
     * 任务描述
     */
    default String description() {
        return "DEFAULT";
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


    default LogConfig logConfig() {
        //参考 log type {@link tech.powerjob.common.enums.LogType}
        //参考 log type  {@link tech.powerjob.common.enums.LogLevel}
        return new LogConfig()
                .setLevel(2)
                .setType(1);
    }

    /**
     * 注册后自动执行
     */
    default boolean enable() {
        return true;
    }

}
