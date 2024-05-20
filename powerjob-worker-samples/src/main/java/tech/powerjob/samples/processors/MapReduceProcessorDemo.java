package tech.powerjob.samples.processors;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.common.utils.MapUtils;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.TaskResult;
import tech.powerjob.worker.core.processor.sdk.MapReduceProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MapReduce 处理器示例
 * 控制台参数：{"batchSize": 100, "batchNum": 2}
 *
 * @author tjq
 * @since 2020/4/17
 */
@Slf4j
@Component("demoMapReduceProcessor")
public class MapReduceProcessorDemo implements MapReduceProcessor {

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        // PowerJob 提供的日志 API，可支持在控制台指定多种日志模式（在线查看 / 本地打印）。最佳实践：全部使用 OmsLogger 打印日志，开发阶段控制台配置为 在线日志方便开发；上线后调整为本地日志，与直接使用 SLF4J 无异
        OmsLogger omsLogger = context.getOmsLogger();

        // 是否为根任务，一般根任务进行任务的分发
        boolean isRootTask = isRootTask();
        // Task 名称，除了 MAP 任务其他 taskName 均由开发者自己创建，某种意义上也可以按参数理解（比如多层 MAP 的情况下，taskName 可以命名为，Map_Level1, Map_Level2，最终按 taskName 判断层级进不同的执行分支）
        String taskName = context.getTaskName();
        // 任务参数，控制台任务配置中直接填写的参数
        String jobParamsStr = context.getJobParams();
        // 任务示例参数，运行任务时手动填写的参数（等同于 OpenAPI runJob 的携带的参数）
        String instanceParamsStr = context.getInstanceParams();

        omsLogger.info("[MapReduceDemo] [startExecuteNewTask] jobId:{}, instanceId:{}, taskId:{}, taskName: {}, RetryTimes: {}, isRootTask:{}, jobParams:{}, instanceParams:{}", context.getJobId(), context.getInstanceId(), context.getTaskId(), taskName, context.getCurrentRetryTimes(), isRootTask, jobParamsStr, instanceParamsStr);

        // 常见写法，优先从 InstanceParams 获取参数，取不到再从 JobParams 中获取，灵活性最佳（相当于实现了实例参数重载任务参数）
        String finalParams = StringUtils.isEmpty(instanceParamsStr) ? jobParamsStr : instanceParamsStr;
        final JSONObject params = Optional.ofNullable(finalParams).map(JSONObject::parseObject).orElse(new JSONObject());

        if (isRootTask) {

            omsLogger.info("[MapReduceDemo] [RootTask] start execute root task~");

            /*
             * rootTask 内的核心逻辑，即为按自己的业务需求拆分子任务。比如
             *  - 从数据库/数仓拉一批任务出来做计算，那 MAP 任务就可以 stream 读全库，每 N 个 ID 作为一个 SubTask 对外分发
             *  - 需要读取几千万个文件进行解析，那么 MAP 任务就可以将 N 个文件名作为一个 SubTask 对外分发，每个子任务接收到文件名称进行文件处理
             *
             * eg. 现在需要从文件中读取100W个ID，并处理数据库中这些ID对应的数据，那么步骤如下：
             * 1. 根任务（RootTask）读取文件，流式拉取100W个ID，并按100个一批的大小组装成子任务进行派发
             * 2. 非根任务获取子任务，完成业务逻辑的处理
             *
             * 以下 demo 进行该逻辑的模拟
             */


            // 构造子任务

            // 需要读取的文件总数
            Long num = MapUtils.getLong(params, "num", 100000L);
            // 每个子任务携带多少个文件ID（此参数越大，每个子任务就“越大”，如果失败的重试成本就越高。参数越小，每个子任务就越轻，当相应的分片数量会提升，会让 PowerJob 计算开销增大，建议按业务需求合理调配）
            Long batchSize = MapUtils.getLong(params, "batchSize", 100L);

            // 此处模拟从文件读取 num 个 ID，每个子任务携带 batchSize 个 ID 作为一个分片
            List<Long> ids = Lists.newArrayList();
            for (long i = 0; i < num; i++) {
                ids.add(i);

                if (ids.size() >= batchSize) {

                    // 构造自己的子任务，自行传递所有需要的参数
                    SubTask subTask = new SubTask(ThreadLocalRandom.current().nextLong(), Lists.newArrayList(ids), "extra");
                    ids.clear();

                    try {
                        /*
                        第一个参数：List<子任务>，map 支持批量操作以减少网络 IO 提升性能，简单起见此处不再示例，开发者可自行优化性能
                        第二个参数：子任务名称，即后续 Task 执行时从 TaskContext#taskName 拿到的值。某种意义上也可以按参数理解（比如多层 MAP 的情况下，taskName 可以命名为，Map_Level1, Map_Level2，最终按 taskName 判断层级进不同的执行分支）
                         */
                        map(Lists.newArrayList(subTask), "L1_FILE_PROCESS");
                    } catch (Exception e) {
                        // 注意 MAP 操作可能抛出异常，建议进行捕获并按需处理
                        omsLogger.error("[MapReduceDemo] map task failed!", e);
                        throw e;
                    }
                }
            }

            if (!ids.isEmpty()) {
                map(Lists.newArrayList(new SubTask()), "L1_FILE_PROCESS");
            }

            // map 阶段的结果，由于前置逻辑为异常直接抛出，执行到这里一定成功，所以无脑设置为 success。开发者可自行调整逻辑
            return new ProcessResult(true, "MAP_SUCCESS,totalNum:" + num);

        }

        // 如果是简单的二层结构（ROOT - SubTASK），此处一定是子 Task，无需再次判断。否则可使用 TaskContext#taskName 字符串匹配 或 TaskContext#SubTask 对象内自定义参数匹配，进入目标执行分支

        // 获取前置节点 map 传递过来的参数，进行业务处理
        SubTask subTask = (SubTask) context.getSubTask();
        log.info("[MapReduceDemo] [SubTask] taskId:{}, taskName: {}, subTask: {}", context.getTaskId(), taskName, JsonUtils.toJSONString(subTask));
        Thread.sleep(MapUtils.getLong(params, "bizProcessCost", 233L));

        // 模拟有成功有失败的情况，开发者按真实业务执行情况判断即可
        long successRate = MapUtils.getLong(params, "successRate", 80L);
        long randomNum = ThreadLocalRandom.current().nextLong(100);
        if (successRate > randomNum) {
            return new ProcessResult(true, "PROCESS_SUCCESS:" + randomNum);
        } else {
            return new ProcessResult(false, "PROCESS_FAILED:" + randomNum);
        }
    }

    @Override
    public ProcessResult reduce(TaskContext context, List<TaskResult> taskResults) {

        // 子任务结果太大，上报在线日志会有 IO 问题，直接使用本地日志打
        log.info("List<TaskResult>: {}", JSONObject.toJSONString(taskResults));

        OmsLogger omsLogger = context.getOmsLogger();
        omsLogger.info("================ MapReduceProcessorDemo#reduce ================");

        // 所有 Task 执行结束后，reduce 将会被执行，taskResults 保存了所有子任务的执行结果。（注意 reduce 由于保存了所有子任务的执行结果，在子任务规模巨大时对内存有极大开销，超大型计算任务慎用或使用流式 reduce（开发中））

        // 用法举例：统计执行结果
        AtomicLong successCnt = new AtomicLong(0);
        AtomicLong failedCnt = new AtomicLong(0);
        taskResults.forEach(tr -> {
            if (tr.isSuccess()) {
                successCnt.incrementAndGet();
            } else {
                failedCnt.incrementAndGet();
            }
        });


        double successRate = 1.0 * successCnt.get() / (successCnt.get() + failedCnt.get());

        String resultMsg = String.format("succeedTaskNum:%d,failedTaskNum:%d,successRate:%f", successCnt.get(), failedCnt.get(), successRate);
        omsLogger.info("[MapReduceDemo] [Reduce] {}", resultMsg);

        // reduce 阶段的结果，将作为任务真正执行结果
        if (successRate > 0.8) {
            return new ProcessResult(true, resultMsg);
        } else {
            return new ProcessResult(false, resultMsg);
        }

    }


    /**
     * 自定义的子任务，按自己的业务需求定义即可
     * 注意：代表子任务参数的类：一定要有无参构造方法！一定要有无参构造方法！一定要有无参构造方法！
     * 最好把 GET / SET 方法也加上，减少序列化问题的概率
     */
    @Data
    @AllArgsConstructor
    public static class SubTask implements Serializable {

        /**
         * 再次强调，一定要有无参构造方法
         */
        public SubTask() {
        }

        private Long siteId;

        private List<Long> idList;

        private String extra;
    }
}
