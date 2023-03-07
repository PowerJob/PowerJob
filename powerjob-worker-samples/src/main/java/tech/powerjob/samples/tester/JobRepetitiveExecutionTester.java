package tech.powerjob.samples.tester;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 简单任务重复执行检测
 * 有用户反馈偶尔存在重试现象，写一个 Processor 来测试
 *
 * @author tjq
 * @since 2023/3/7
 */
@Slf4j
@Component
public class JobRepetitiveExecutionTester implements BasicProcessor {

    private final AtomicLong repetitions = new AtomicLong();

    /**
     * 存储 jobId_instanceId 方便查问题
     * 测试代码，就不考虑内存泄漏了
     */
    private final Set<String> repetitionsInfo = Sets.newHashSet();
    private final Cache<String, Integer> instanceId2Num = CacheBuilder.newBuilder().maximumSize(1024).build();

    @Override
    public ProcessResult process(TaskContext context) throws Exception {

        // 纯本地日志打印当前情况
        log.info("[SimpleJobRepetitiveExecutionTester] repetitions:{}, repetitionsInfo: {}", repetitions.get(), repetitionsInfo);

        final OmsLogger omsLogger = context.getOmsLogger();
        final Long instanceId = context.getInstanceId();
        omsLogger.info("[SimpleJobRepetitiveExecutionTester] jobId: {}, instanceId: {}, subInstanceId: {}", context.getJobParams(), instanceId, context.getSubInstanceId());
        check(context);
        return new ProcessResult(true, "success: " + System.currentTimeMillis());
    }

    private synchronized void check(TaskContext context) {
        String uid = context.getInstanceId() + "_" + Optional.ofNullable(context.getSubInstanceId()).orElse(context.getInstanceId());
        Integer numIfPresent = instanceId2Num.getIfPresent(uid);
        // 不重复情况下，100% 进入该分支
        if (numIfPresent == null) {
            instanceId2Num.put(uid, 1);
            return;
        }
        context.getOmsLogger().error("[Repetitions] instance[id={}] already execute {} nums!", uid, numIfPresent);
        instanceId2Num.put(uid, numIfPresent + 1);
        repetitionsInfo.add(String.format("%d_%s", context.getJobId(), uid));
        context.getOmsLogger().error("[Repetitions] current repetitions num: {}", repetitions.incrementAndGet());
        context.getOmsLogger().error("[Repetitions] current repetitionsInfo: {}", repetitionsInfo.toString());
    }
}
