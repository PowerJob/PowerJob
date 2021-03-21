package tech.powerjob.worker.test.processors;

import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.sdk.MapReduceProcessor;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

/**
 * 测试 MapReduce 处理器
 *
 * @author tjq
 * @since 2020/3/24
 */
public class TestMapReduceProcessor implements MapReduceProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {
        System.out.println("============== TestMapReduceProcessor#process ==============");
        System.out.println("isRootTask:" + isRootTask());
        System.out.println("taskContext:" + JsonUtils.toJSONString(context));

        if (isRootTask()) {
            System.out.println("==== MAP ====");
            List<TestSubTask> subTasks = Lists.newLinkedList();
            for (int j = 0; j < 2; j++) {
                for (int i = 0; i < 100; i++) {
                    int x = j * 100 + i;
                    subTasks.add(new TestSubTask("name" + x, x));
                }
                map(subTasks, "MAP_TEST_TASK");
                subTasks.clear();
            }
            return new ProcessResult(true, "MAP_SUCCESS");
        }else {
            System.out.println("==== NORMAL_PROCESS ====");
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
        System.out.println("============== TestMapReduceProcessor#reduce ==============");
        System.out.println("taskContext:" + JsonUtils.toJSONString(context));
        System.out.println("taskId2Result:" + taskResults);
        return new ProcessResult(true, "REDUCE_SUCCESS");
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
