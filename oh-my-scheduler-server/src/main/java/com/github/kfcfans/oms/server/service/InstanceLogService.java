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
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
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

    // 直接操作 mongoDB 文件系统
    private GridFsTemplate gridFsTemplate;
    private LocalInstanceLogRepository localInstanceLogRepository;

    // 本地维护了在线日志的任务实例ID
    private final Set<Long> instanceIds = Sets.newConcurrentHashSet();
    // 锁（可重入锁也太坑了吧，需要考虑同一个线程重复下载的问题 -> 因为下载交给了额外的线程去做...）
    private final int lockNum;
    private final Lock[] locks;
    private final Executor workerPool;

    // 格式化时间戳
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSS");
    // 用户路径
    private static final String USER_HOME = System.getProperty("user.home", "oms");
    // 每一个展示的行数
    private static final int MAX_LINE_COUNT = 500;
    // 过期时间
    private static final long EXPIRE_INTERVAL_MS = 60000;
    // 小文件阈值(2M)
    private static final int SMALL_FILE_MAX_SIZE = 2 * 1024 * 1024;

    public InstanceLogService() {
        lockNum = Runtime.getRuntime().availableProcessors();
        locks = new ReentrantLock[lockNum];
        for (int i = 0; i < lockNum; i++) {
            locks[i] = new ReentrantLock();
        }

        workerPool = new ThreadPoolExecutor(lockNum, lockNum, 1, TimeUnit.MINUTES, Queues.newLinkedBlockingQueue());
    }

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
        Lock lock = locks[(int) (instanceId % lockNum)];

        lock.lock();
        try {
            long logCount = localInstanceLogRepository.countByInstanceId(instanceId);

            // 直接从本地数据库构建日志文件
            if (logCount != 0) {

                // 存在则判断上次更改时间，1分钟内有效
                if (logFile.exists()) {
                    long offset = System.currentTimeMillis() - logFile.lastModified();
                    // 过期才选择重新构建文件
                    if (offset > EXPIRE_INTERVAL_MS) {
                        Stream<LocalInstanceLogDO> logStream = localInstanceLogRepository.findByInstanceIdOrderByLogTime(instanceId);

                        // 这里直接用 Controller 线程执行，毕竟本地持久化用不了多少时间，因此不用考虑可重入锁的问题
                        stream2File(logStream, instanceId);
                    }
                }

            }else {

                if (gridFsTemplate == null) {
                    return StringPage.simple("There is no local log for this task now, you need to use mongoDB to store the past logs.");
                }

                // 不存在，需要重新下载
                if (!logFile.exists()) {
                    GridFsResource gridFsResource = gridFsTemplate.getResource(genLogFileName(instanceId));

                    if (!gridFsResource.exists()) {
                        return StringPage.simple("There is no online log for this job instance.");
                    }

                    long targetFileSize = gridFsResource.contentLength();
                    // 小文件 Controller 线程直接执行，大文件则异步下载，先返回 downloading...
                    if (targetFileSize <= SMALL_FILE_MAX_SIZE) {
                        gridFs2File(gridFsResource, logFile);
                    }else {
                        workerPool.execute(() -> gridFs2File(gridFsResource, logFile));
                        return StringPage.simple("downloading from mongoDB, please retry some time later~");
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
        }finally {
            lock.unlock();
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
    private void stream2File(Stream<LocalInstanceLogDO> stream, long instanceId) {
        File logFile = new File(genLogFilePath(instanceId));
        if (!logFile.getParentFile().exists()) {
            if (!logFile.getParentFile().mkdirs()) {
                log.warn("[InstanceLogService] create dir for instanceLog failed, path is {}.", logFile.getPath());
                return;
            }
        }
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
     * 将MongoDB中存储的日志持久化为磁盘日志
     * @param gridFsResource mongoDB 文件资源
     * @param logFile 本地文件资源
     */
    private void gridFs2File(GridFsResource gridFsResource, File logFile) {
        byte[] buffer = new byte[1024];
        try (BufferedInputStream gis = new BufferedInputStream(gridFsResource.getInputStream());
             BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(logFile))
        ) {
            while (gis.read(buffer) != -1) {
                bos.write(buffer);
            }
            bos.flush();
        }catch (Exception e) {
            log.warn("[InstanceLogService] download instanceLog to local file({}) failed.", logFile.getName(), e);
        }
    }



    /**
     * 拼接日志 -> 2020-04-29 22:07:10.059 192.168.1.1:2777 INFO XXX
     * @param instanceLog 日志对象
     * @return 字符串
     */
    private static String convertLog(LocalInstanceLogDO instanceLog) {
        String pattern = "%s [%s] -%s";
        return String.format(pattern, dateFormat.format(instanceLog.getLogTime()), instanceLog.getLogContent());
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
    @Autowired
    public void setLocalInstanceLogRepository(LocalInstanceLogRepository localInstanceLogRepository) {
        this.localInstanceLogRepository = localInstanceLogRepository;
    }
}
