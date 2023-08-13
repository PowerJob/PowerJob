package tech.powerjob.official.processors.impl;

import com.alibaba.fastjson.JSONObject;
import lombok.*;
import org.apache.commons.lang3.RandomStringUtils;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.official.processors.CommonBasicProcessor;
import tech.powerjob.official.processors.util.CommonUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.sdk.BroadcastProcessor;
import tech.powerjob.worker.core.processor.sdk.MapReduceProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 功能验证用处理器，帮助用户快速验证想要测试的功能
 *
 * @author tjq
 * @since 2023/8/13
 */
public class VerificationProcessor extends CommonBasicProcessor implements MapReduceProcessor, BroadcastProcessor {

    @Override
    public ProcessResult preProcess(TaskContext context) throws Exception {
        return new ProcessResult(true, "preProcess successfully!");
    }

    @Override
    protected ProcessResult process0(TaskContext taskContext) throws Exception {

        final OmsLogger omsLogger = taskContext.getOmsLogger();

        final String paramsStr = CommonUtils.parseParams(taskContext);
        final VerificationParam verificationParam = JSONObject.parseObject(paramsStr, VerificationParam.class);

        final Mode mode = Mode.of(verificationParam.getMode());

        switch (mode) {
            case ERROR:
                return new ProcessResult(false, "EXECUTE_FAILED_DUE_TO_CONFIG");
            case EXCEPTION:
                throw new PowerJobException("exception for test");
            case TIMEOUT:
                final Long sleepMs = Optional.ofNullable(verificationParam.getSleepMs()).orElse(3600000L);
                Thread.sleep(sleepMs);
                return new ProcessResult(true, "AFTER_SLEEP_" + sleepMs);
            case MR:
                if (isRootTask()) {
                    final int batchNum = Optional.ofNullable(verificationParam.getBatchNum()).orElse(10);
                    final int batchSize = Optional.ofNullable(verificationParam.getBatchSize()).orElse(100);
                    omsLogger.info("[VerificationProcessor] start root task~");
                    List<TestSubTask> subTasks = new ArrayList<>();
                    for (int a = 0; a < batchNum; a++) {
                        for (int b = 0; b < batchSize; b++) {
                            int x = a * batchSize + b;
                            subTasks.add(new TestSubTask("task_" + x, x));
                        }
                        map(subTasks, "MAP_TEST_TASK_" + a);
                        omsLogger.info("[VerificationProcessor] [{}] map one batch successfully~", batchNum);
                        subTasks.clear();
                    }
                    omsLogger.info("[VerificationProcessor] all map successfully!");
                    return new ProcessResult(true, "MAP_SUCCESS");
                } else {
                    final Double successRate = Optional.ofNullable(verificationParam.getSubTaskSuccessRate()).orElse(0.5);
                    final double rd = ThreadLocalRandom.current().nextDouble(0, 1);
                    boolean success = rd <= successRate;
                    return new ProcessResult(success, String.format("taskId_%s_success_%s", taskContext.getTaskId(), success));
                }
        }

        return new ProcessResult(true, "EXECUTE_SUCCESSFULLY_" + RandomStringUtils.randomAlphanumeric(Optional.ofNullable(verificationParam.getResponseSize()).orElse(10)));
    }

    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {
        return new ProcessResult(true, "REDUCE_SUCCESS");
    }

    enum Mode {
        /**
         * 常规模式，直接返回响应
         */
        BASE,
        /**
         * 超时，sleep 一段时间测试超时控制
         */
        TIMEOUT,
        /**
         * 测试执行失败，响应返回 success = false
         */
        ERROR,
        /**
         * 测试执行异常，抛出异常
         */
        EXCEPTION,
        /**
         * MapReduce
         */
        MR
        ;

        public static Mode of(String v) {
            for (Mode m : values()) {
                if (m.name().equalsIgnoreCase(v)) {
                    return m;
                }
            }
            return Mode.BASE;
        }
    }

    @Data
    public static class VerificationParam implements Serializable {
        /**
         * 验证模式
         */
        private String mode;
        /**
         * 休眠时间，用于验证超时
         */
        private Long sleepMs;
        /**
         * 【MR】批次大小，用于验证 MapReduce
         */
        private Integer batchSize;
        /**
         * 【MR】batchNum
         */
        private Integer batchNum;
        /**
         * 【MR】子任务成功率
         */
        private Double subTaskSuccessRate;

        private Integer responseSize;
    }

    @Getter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestSubTask {
        private String taskName;
        private int id;
    }
}
