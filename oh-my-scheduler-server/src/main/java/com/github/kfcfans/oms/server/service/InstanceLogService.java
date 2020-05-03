package com.github.kfcfans.oms.server.service;

import com.github.kfcfans.oms.common.TimeExpressionType;
import com.github.kfcfans.oms.common.model.InstanceLogContent;
import com.github.kfcfans.oms.common.utils.CommonUtils;
import com.github.kfcfans.oms.server.persistence.StringPage;
import com.github.kfcfans.oms.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.local.LocalInstanceLogDO;
import com.github.kfcfans.oms.server.persistence.local.LocalInstanceLogRepository;
import com.github.kfcfans.oms.server.persistence.mongodb.InstanceLogMetadata;
import com.github.kfcfans.oms.server.service.instance.InstanceManager;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSDownloadStream;
import com.mongodb.client.gridfs.model.GridFSFile;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.MongoDbFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsCriteria;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ResourceUtils;

import javax.annotation.Resource;
import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
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

    private GridFsTemplate gridFsTemplate;
    @Resource
    private LocalInstanceLogRepository localInstanceLogRepository;

    // 本地维护了在线日志的任务实例ID
    private final Set<Long> instanceIds = Sets.newConcurrentHashSet();

    private static final String SPACE = " ";
    private static final String TIME_PATTERN = "yyyy-MM-dd HH:mm:ss.SSS";

    private static final FastDateFormat dateFormat = FastDateFormat.getInstance(TIME_PATTERN);

    // 文件路径
    private static final String USER_HOME = System.getProperty("user.home", "oms");
    // 每一个展示的行数
    private static final int MAX_LINE_COUNT = 500;

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
     * 获取任务实例运行日志（默认存在本地数据，需要由生成完成请求的路由与转发）
     * @param instanceId 任务实例ID
     * @param index 页码
     * @return 文本字符串
     */
    @Transactional(readOnly = true)
    public StringPage fetchInstanceLog(Long instanceId, long index) {

        File logFile = new File(genLogFilePath(instanceId));

        try {

            long logCount = localInstanceLogRepository.countByInstanceId(instanceId);

            // 构建本地日志文件
            if (logCount != 0) {

                // 数据库中存在，说明数据还在更新中，需要重新生成
                Stream<LocalInstanceLogDO> logStream = localInstanceLogRepository.findByInstanceIdOrderByLogTime(instanceId);
                stream2File(logStream, instanceId);

            }else {

                if (gridFsTemplate == null) {
                    return StringPage.simple("There is no local log for this task now, you need to use mongoDB to store the logs.");
                }

                // 不存在，需要重新下载
                if (!logFile.exists()) {
                    GridFsResource gridFsResource = gridFsTemplate.getResource(genLogFileName(instanceId));

                    if (!gridFsResource.exists()) {
                        return StringPage.simple("There is no online log for this job instance");
                    }

                    byte[] buffer = new byte[1024];
                    try (BufferedInputStream gis = new BufferedInputStream(gridFsResource.getInputStream());
                         BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(logFile))
                    ) {
                        while (gis.read(buffer) != -1) {
                            bos.write(buffer);
                        }
                    }
                }
            }

            if (!logFile.exists()) {
                return StringPage.simple("There is no online log for this task instance now.");
            }

            // 分页展示数据
            long lines = 0;
            StringBuilder sb = new StringBuilder();
            String lineStr;
            long left = index * MAX_LINE_COUNT;
            long right = left + MAX_LINE_COUNT;
            try (LineNumberReader lr = new LineNumberReader(new FileReader(logFile))) {
                while ((lineStr = lr.readLine()) != null) {

                    // 指定范围内，读出
                    if (lines >= left && lines < right) {
                        sb.append(lineStr).append(System.lineSeparator());
                    }
                    ++lines;
                }
            }catch (Exception e) {
                log.warn("[InstanceLogService] read logFile from disk failed.", e);
            }

            double totalPage = Math.ceil(1.0 * lines / MAX_LINE_COUNT);
            return new StringPage(index, (long) totalPage, sb.toString());


        }catch (Exception e) {
            log.error("[InstanceLogService] fetchInstanceLog for instance(instanceId={}) failed.", instanceId, e);
            return StringPage.simple("unknown error from oms-server, please see oms-server's log to find the problem");
        }
    }

    /**
     * 将本地的任务实例运行日志同步到 mongoDB 存储，在任务执行结束后异步执行
     * @param instanceId 任务实例ID
     */
    @Transactional
    @Async("omsCommonPool")
    public void sync(Long instanceId) {

        // 休眠10秒等待全部数据上报（OmsLogHandler 每隔5秒上报数据）
        try {
            TimeUnit.SECONDS.sleep(10);
        }catch (Exception ignore) {
        }

        Stopwatch sw = Stopwatch.createStarted();

        if (gridFsTemplate != null) {

            File logFile = new File(genLogFilePath(instanceId));

            // 先持久化到本地磁盘
            try {
                Stream<LocalInstanceLogDO> allLogStream = localInstanceLogRepository.findByInstanceIdOrderByLogTime(instanceId);
                stream2File(allLogStream, instanceId);
            }catch (Exception e) {
                log.warn("[InstanceLogService] get log stream failed for instance(instanceId={}).", instanceId, e);
            }

            // 推送到 mongoDB
            try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(logFile))) {

                InstanceLogMetadata metadata = new InstanceLogMetadata();
                metadata.setInstanceId(instanceId);
                metadata.setFileSize(logFile.length());
                metadata.setCreatedTime(System.currentTimeMillis());

                gridFsTemplate.store(bis, genLogFileName(instanceId), metadata);

            }catch (Exception e) {
                log.warn("[InstanceLogService] push local instanceLogs(instanceId={}) to mongoDB failed.", instanceId, e);
            }
        }

        // 删除本地数据
        try {
            long total = CommonUtils.executeWithRetry0(() -> localInstanceLogRepository.deleteByInstanceId(instanceId));

            instanceIds.remove(instanceId);
            log.info("[InstanceLogService] sync local instanceLogs(instanceId={}) to mongoDB succeed, total logs: {},using: {}.", instanceId, total, sw.stop());
        }catch (Exception e) {
            log.warn("[InstanceLogService] delete local instanceLogs failed.", e);
        }
    }

    /**
     * 将数据库中存储的日志流转化为磁盘日志文件
     * @param stream 流
     * @param instanceId 任务实例ID
     */
    @SuppressWarnings("all")
    private void stream2File(Stream<LocalInstanceLogDO> stream, long instanceId) {
        File logFile = new File(genLogFilePath(instanceId));
        logFile.getParentFile().mkdirs();
        try (FileWriter fw = new FileWriter(logFile); BufferedWriter bfw = new BufferedWriter(fw)) {
            stream.forEach(instanceLog -> {
                try {
                    bfw.write(convertLog(instanceLog) + System.lineSeparator());
                }catch (Exception ignore) {
                }
            });
        }catch (Exception e) {
            log.warn("[InstanceLogService] write instanceLog(instanceId={}) to local file failed.", instanceId, e);
        }finally {
            stream.close();
        }
    }



    /**
     * 拼接日志 -> 2020-04-29 22:07:10.059 192.168.1.1:2777 INFO XXX
     * @param instanceLog 日志对象
     * @return 字符串
     */
    private static String convertLog(LocalInstanceLogDO instanceLog) {
        return dateFormat.format(instanceLog.getLogTime()) + SPACE + instanceLog.getWorkerAddress() + SPACE + instanceLog.getLogContent();
    }


    @Async("omsTimingPool")
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

    private static String genLogFileName(long instanceId) {
        return String.format("%d.log", instanceId);
    }
    private static String genLogFilePath(long instanceId) {
        return USER_HOME + "/oms/online_log/" + genLogFileName(instanceId);
    }

    @Autowired(required = false)
    public void setGridFsTemplate(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }
}
