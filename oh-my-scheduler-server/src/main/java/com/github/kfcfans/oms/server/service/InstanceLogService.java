package com.github.kfcfans.oms.server.service;

import com.github.kfcfans.common.TimeExpressionType;
import com.github.kfcfans.common.model.InstanceLogContent;
import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.oms.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.local.LocalInstanceLogDO;
import com.github.kfcfans.oms.server.persistence.local.LocalInstanceLogRepository;
import com.github.kfcfans.oms.server.persistence.mongodb.InstanceLogDO;
import com.github.kfcfans.oms.server.service.instance.InstanceManager;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 任务实例运行时日志服务
 *
 * @author tjq
 * @since 2020/4/27
 */
@Slf4j
@Service
public class InstanceLogService {

    @Resource
    private MongoTemplate mongoTemplate;
    @Resource
    private LocalInstanceLogRepository localInstanceLogRepository;

    // 本地维护了在线日志的任务实例ID
    private final Set<Long> instanceIds = Sets.newConcurrentHashSet();

    private static final String SPACE = " ";
    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final int BATCH_SIZE = 1000;

    /**
     * 提交日志记录，持久化到本地数据库中
     * @param workerAddress 上报机器地址
     * @param logs 任务实例运行时日志
     */
    public void submitLogs(String workerAddress, List<InstanceLogContent> logs) {

        List<LocalInstanceLogDO> logList = logs.stream().map(x -> {
            instanceIds.add(x.getInstanceId());

            LocalInstanceLogDO y = new LocalInstanceLogDO();
            BeanUtils.copyProperties(x, y);
            y.setWorkerAddress(workerAddress);
            return y;
        }).collect(Collectors.toList());

        try {
            CommonUtils.executeWithRetry0(() -> localInstanceLogRepository.saveAll(logList));
        }catch (Exception e) {
            log.warn("[InstanceLogService] persistent instance logs failed, these logs will be dropped: {}.", logs, e);
        }
    }

    /**
     * 将本地的任务实例运行日志同步到 mongoDB 存储，在任务执行结束后异步执行
     * @param instanceId 任务实例ID
     */
    @Async("commonTaskExecutor")
    public void sync(Long instanceId) {

        // 休眠10秒等待全部数据上报（OmsLogHandler 每隔5秒上报数据）
        try {
            TimeUnit.SECONDS.sleep(10);
        }catch (Exception ignore) {
        }

        Stopwatch sw = Stopwatch.createStarted();
        FastDateFormat dateFormat = FastDateFormat.getInstance(TIME_PATTERN);

        // 流式操作避免 OOM，至少要扛住 1000W 条日志记录的写入（需要测试时监控内存变化）
        Stream<LocalInstanceLogDO> allLogs = localInstanceLogRepository.findByInstanceIdOrderByLogTime(instanceId);

        List<String> instanceLogs = Lists.newLinkedList();
        AtomicLong counter = new AtomicLong(0);
        AtomicBoolean initialized = new AtomicBoolean(false);

        // 将整库数据写入 MongoDB
        allLogs.forEach(instanceLog -> {
            counter.incrementAndGet();

            // 拼接日志 -> 2019-4-21 00:00:00.000 192.168.1.1:2777  INFO XXX
            String logStr = dateFormat.format(instanceLog.getLogTime()) + SPACE + instanceLog.getWorkerAddress() + SPACE + instanceLog.getLogContent();
            instanceLogs.add(logStr);

            if (instanceLogs.size() > BATCH_SIZE) {
                saveToMongoDB(instanceId, instanceLogs, initialized);
            }
        });

        if (!instanceLogs.isEmpty()) {
            saveToMongoDB(instanceId, instanceLogs, initialized);
        }

        // 删除本地数据
        try {
            CommonUtils.executeWithRetry0(() -> localInstanceLogRepository.deleteByInstanceId(instanceId));

            instanceIds.remove(instanceId);
            log.debug("[InstanceLogService] sync local instanceLogs to mongoDB succeed, total logs: {},using: {}.", counter.get(), sw.stop());
        }catch (Exception e) {
            log.warn("[InstanceLogService] delete local instanceLogs failed.", e);
        }
    }

    private void saveToMongoDB(Long instanceId, List<String> logList, AtomicBoolean initialized) {

        try {
            CommonUtils.executeWithRetry0(() -> {
                if (initialized.get()) {
                    Query mongoQuery = Query.query(Criteria.where("instanceId").is(instanceId));
                    Update mongoUpdate = new Update().push("logList").each(logList);
                    mongoTemplate.updateFirst(mongoQuery, mongoUpdate, InstanceLogDO.class);
                }else {
                    InstanceLogDO newInstanceLog = new InstanceLogDO();
                    newInstanceLog.setInstanceId(instanceId);
                    newInstanceLog.setLogList(logList);
                    mongoTemplate.save(newInstanceLog);
                    initialized.set(true);
                }
                logList.clear();
                return null;
            });
        }catch (Exception e) {
            log.warn("[InstanceLogService] push instanceLog(instanceId={},logList={}) to mongoDB failed.", instanceId, logList, e);
        }
    }

    @Async("timingTaskExecutor")
    @Scheduled(fixedDelay = 60000)
    public void timingCheck() {

        // 1. 定时删除秒级任务的日志
        List<Long> frequentInstanceIds = Lists.newLinkedList();
        instanceIds.forEach(instanceId -> {
            JobInfoDO jobInfo = InstanceManager.fetchJobInfo(instanceId);
            if (jobInfo == null) {
                return;
            }

            if (TimeExpressionType.frequentTypes.contains(jobInfo.getTimeExpressionType())) {
                frequentInstanceIds.add(instanceId);
            }
        });

        if (!CollectionUtils.isEmpty(frequentInstanceIds)) {
            // 只保留最近10分钟的日志
            long time = System.currentTimeMillis() - 10 * 60 * 1000;
            Lists.partition(frequentInstanceIds, 100).forEach(p -> {
                try {
                    localInstanceLogRepository.deleteByInstanceIdInAndLogTimeLessThan(p, time);
                }catch (Exception e) {
                    log.warn("[InstanceLogService] delete expired logs for instance: {} failed.", p, e);
                }
            });
        }
    }
}
