package tech.powerjob.samples.processors;

import lombok.extern.slf4j.Slf4j;
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


        if (isRootTask()) {
            log.warn("[MapProcessor:MAP],taskId = {} !!!!!!!!!!!!!!",context.getTaskId());
            List<SubTask> subTasks = Lists.newLinkedList();
            for (int j = 0; j < BATCH_NUM; j++) {
                SubTask subTask = new SubTask();
                subTask.siteId = j;
                subTask.itemIds = Lists.newLinkedList();
                subTasks.add(subTask);
                for (int i = 0; i < BATCH_SIZE; i++) {
                    subTask.itemIds.add(i);
                }
            }
            map(subTasks, "MAP_TEST_TASK");
            return new ProcessResult(true, "map successfully");
        }else {
            // 测试在 Map 任务中追加上下文
            context.getWorkflowContext().appendData2WfContext("Yasuo","A sword's poor company for a long road.");
            boolean b = ThreadLocalRandom.current().nextBoolean();
            log.warn("[MapProcessor:PROCESS],taskId = {},result={} !!!!!!!!!!!!!!",context.getTaskId(),b);
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
