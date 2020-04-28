package com.github.kfcfans.oms.server.service;

import com.github.kfcfans.common.model.InstanceLogContent;
import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.oms.server.persistence.local.LocalInstanceLogDO;
import com.github.kfcfans.oms.server.persistence.local.LocalInstanceLogRepository;
import com.github.kfcfans.oms.server.persistence.mongodb.InstanceLogDO;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
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
    @Async
    public void sync(Long instanceId) {

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
        }catch (Exception e) {
            log.warn("[InstanceLogService] delete local instanceLogs failed.", e);
        }

        log.debug("[InstanceLogService] sync local instanceLogs to mongoDB succeed, total logs: {},using: {}.", counter.get(), sw.stop());
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
}
