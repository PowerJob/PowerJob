package com.github.kfcfans.powerjob.samples.processors;

import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.samples.MysteryService;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.github.kfcfans.powerjob.worker.core.processor.TaskContext;
import com.github.kfcfans.powerjob.worker.core.processor.sdk.MapProcessor;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Map处理器 示例
 *
 * @author tjq
 * @since 2020/4/18
 */
@Component
public class MapProcessorDemo extends MapProcessor {

    @Resource
    private MysteryService mysteryService;

    // 每一批发送任务大小
    private static final int batchSize = 100;
    // 发送的批次
    private static final int batchNum = 2;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        System.out.println("============== MapProcessorDemo#process ==============");
        System.out.println("isRootTask:" + isRootTask());
        System.out.println("taskContext:" + JsonUtils.toJSONString(context));
        System.out.println(mysteryService.hasaki());

        if (isRootTask()) {
            System.out.println("==== MAP ====");
            List<SubTask> subTasks = Lists.newLinkedList();
            for (int j = 0; j < batchNum; j++) {
                SubTask subTask = new SubTask();
                subTask.siteId = j;
                subTask.itemIds = Lists.newLinkedList();
                subTasks.add(subTask);
                for (int i = 0; i < batchSize; i++) {
                    subTask.itemIds.add(i);
                }
            }
            return map(subTasks, "MAP_TEST_TASK");
        }else {
            System.out.println("==== PROCESS ====");
            System.out.println("subTask: " + JsonUtils.toJSONString(context.getSubTask()));
            boolean b = ThreadLocalRandom.current().nextBoolean();
            return new ProcessResult(b, "RESULT:" + b);
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SubTask {
        private Integer siteId;
        private List<Integer> itemIds;
    }
}
