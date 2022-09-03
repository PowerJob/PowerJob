package tech.powerjob.samples.processors;

import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.samples.MysteryService;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.MapProcessor;
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
public class MapProcessorDemo implements MapProcessor {

    @Resource
    private MysteryService mysteryService;

    /**
     * 每一批发送任务大小
     */
    private static final int BATCH_SIZE = 100;
    /**
     * 发送的批次
     */
    private static final int BATCH_NUM = 5;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        System.out.println("============== MapProcessorDemo#process ==============");
        System.out.println("isRootTask:" + isRootTask());
        System.out.println("taskContext:" + JsonUtils.toJSONString(context));
        System.out.println(mysteryService.hasaki());

        if (isRootTask()) {
            System.out.println("==== MAP ====");
            List<SubTask> subTasks = Lists.newLinkedList();
            for (int j = 0; j < BATCH_NUM; j++) {
                SubTask subTask = new SubTask();
                subTask.siteId = j;
                subTask.itemIds = Lists.newLinkedList();
                subTasks.add(subTask);
                for (int i = 0; i < BATCH_SIZE; i++) {
                    subTask.itemIds.add(i + j * 100);
                }
            }
            map(subTasks, "MAP_TEST_TASK");
            return new ProcessResult(true, "map successfully");
        } else {

            System.out.println("==== PROCESS ====");
            SubTask subTask = (SubTask) context.getSubTask();
            for (Integer itemId : subTask.getItemIds()) {
                if (Thread.interrupted()) {
                    // 任务被中断
                    System.out.println("job has been stop! so stop to process subTask:" + subTask.getSiteId() + "=>" + itemId);
                    break;
                }
                System.out.println("processing subTask: " + subTask.getSiteId() + "=>" + itemId);
                int max = Integer.MAX_VALUE >> 4;
                for (int i = 0; ; i++) {
                    // 模拟耗时操作
                    if (i > max) {
                        break;
                    }
                }
            }
            // 测试在 Map 任务中追加上下文
            context.getWorkflowContext().appendData2WfContext("Yasuo", "A sword's poor company for a long road.");
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
