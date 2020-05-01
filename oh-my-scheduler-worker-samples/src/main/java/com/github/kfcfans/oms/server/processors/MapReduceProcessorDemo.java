package com.github.kfcfans.oms.server.processors;

import com.alibaba.fastjson.JSONObject;
import com.github.kfcfans.oms.common.utils.JsonUtils;
import com.github.kfcfans.oms.worker.core.processor.ProcessResult;
import com.github.kfcfans.oms.worker.core.processor.TaskContext;
import com.github.kfcfans.oms.worker.core.processor.TaskResult;
import com.github.kfcfans.oms.worker.core.processor.sdk.MapReduceProcessor;
import com.github.kfcfans.oms.worker.log.OmsLogger;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * MapReduce 处理器示例
 * com.github.kfcfans.oms.server.processors.MapReduceProcessorDemo
 *
 * @author tjq
 * @since 2020/4/17
 */
@Slf4j
@Component
public class MapReduceProcessorDemo extends MapReduceProcessor {

    // 每一批发送任务大小
    private static final int batchSize = 100;
    // 发送的批次
    private static final int batchNum = 2;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        OmsLogger omsLogger = context.getOmsLogger();

        System.out.println("============== TestMapReduceProcessor#process ==============");
        System.out.println("isRootTask:" + isRootTask());
        System.out.println("taskContext:" + JsonUtils.toJSONString(context));

        if (isRootTask()) {
            System.out.println("==== MAP ====");
            omsLogger.info("[DemoMRProcessor] start root task~");
            List<TestSubTask> subTasks = Lists.newLinkedList();
            for (int j = 0; j < batchNum; j++) {
                for (int i = 0; i < batchSize; i++) {
                    int x = j * batchSize + i;
                    subTasks.add(new TestSubTask("name" + x, x));
                }
                ProcessResult mapResult = map(subTasks, "MAP_TEST_TASK");
                System.out.println("mapResult: " + mapResult);
                subTasks.clear();
            }
            omsLogger.info("[DemoMRProcessor] map success~");
            return new ProcessResult(true, "MAP_SUCCESS");
        }else {
            System.out.println("==== NORMAL_PROCESS ====");
            omsLogger.info("[DemoMRProcessor] normal process~");
            System.out.println("subTask: " + JsonUtils.toJSONString(context.getSubTask()));
            Thread.sleep(1000);
            if (context.getCurrentRetryTimes() == 0) {
                return new ProcessResult(false, "FIRST_FAILED");
            }else {
                return new ProcessResult(true, "PROCESS_SUCCESS");
            }
        }
    }

    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {
        log.info("================ MapReduceProcessorDemo#postProcess ================");
        log.info("TaskContext: {}", JSONObject.toJSONString(context));
        log.info("List<TaskResult>: {}", JSONObject.toJSONString(taskResults));
        context.getOmsLogger().info("MapReduce job finished, result is {}.", taskResults);

        boolean success = ThreadLocalRandom.current().nextBoolean();
        return new ProcessResult(success, context + ": " + success);
    }

    @Getter
    @ToString
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TestSubTask {
        private String name;
        private int age;
    }
}
