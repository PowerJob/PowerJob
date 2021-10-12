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
import tech.powerjob.worker.log.OmsLogger;

import javax.annotation.Resource;
import java.util.LinkedList;
import java.util.List;

/**
 * Map处理器 示例
 *
 * @author tjq
 * @since 2020/4/18
 */
@Component
@Slf4j
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
    private static final int BATCH_NUM = 100000;

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        OmsLogger omsLogger = context.getOmsLogger();
        if (isRootTask()) {
            log.warn("[MapProcessor:MAP],taskId = {} !!!!!!!!!!!!!!",context.getTaskId());
            omsLogger.warn("[MapProcessor:MAP],taskId = {} !!!!!!!!!!!!!!",context.getTaskId());
            List<SubTask> subTasks = Lists.newLinkedList();
            for (int j = 0; j < BATCH_NUM; j++) {
                SubTask subTask = new SubTask();
                subTask.params = new LinkedList<>();
                subTask.siteId = j;
                subTask.itemIds = Lists.newLinkedList();
                subTasks.add(subTask);
                for (int i = 0; i < BATCH_SIZE; i++) {
                    subTask.itemIds.add(i);
                    subTask.params.add("{\"callback\":\"\",//可选，留空则不回调\"state\":\"xxxxxx\",//可选，幂等状态码\"channel\":\"ad\",//传输通道，目前只支持alert/ad，必须\"trigger\":{\"type\":\"immediately\"//固定值，必须},\"recipient\":{\"filters\":[{\"devices\":[{\"os\":{\"platform\":\"ios/android\"//操作系统，可选},\"app\":{\"version\":\"xxxx\",//可选，app版本号\"key\":\"master_ios/master_pro/master_android/mail\",//appKey\"range\":\">=/>/=/<=/<\"//版本范围}}],\"accounts\":{\"account\":[{\"muid\":\"159xxxx\",//大师号，payload中可以用占位符${recipient.filters.accounts.account[#.index].muid}取得\"uid\":\"zhangsan@163.com\"//大师号下的邮箱号，payload中可以用占位符${recipient.filters.accounts.account[#.index].uid}取得\"did\":\"xxxx\"//设备号id，暂时不支持占位符获取},{\"muid\":\"159xxxx\",\"uid\":\"lisi@163.com\"}]}}]},\"body\":{\"title\":\"\",//必须\"icon\":\"icon\",//可选\"content\":\"test\",//必须\"operation\":{},//可选\"format\":\"text\",//目前为固定值，必须\"notify\":[\"bell\"//提醒方式，目前为固定值，可选]}}");
                }
            }
            map(subTasks, "MAP_TEST_TASK");
            return new ProcessResult(true, "map successfully");
        }else {
            SubTask subTask = (SubTask) context.getSubTask();
            // 测试在 Map 任务中追加上下文
            context.getWorkflowContext().appendData2WfContext("Yasuo","A sword's poor company for a long road.");
            log.warn("[MapProcessor:PROCESS],taskId = {},result={},params.size={} !!!!!!!!!!!!!!",context.getTaskId(),true,subTask.getParams().size());
            omsLogger.warn("[MapProcessor:PROCESS],taskId = {},result={} !!!!!!!!!!!!!!",context.getTaskId(),true);
            return new ProcessResult(true, "RESULT:" + true);
        }
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SubTask {
        private Integer siteId;
        private List<Integer> itemIds;
        private List<String> params;
    }
}
