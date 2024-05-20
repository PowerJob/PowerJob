package tech.powerjob.server.core.instance;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.CollectionUtils;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.enums.LogLevel;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.model.InstanceLogContent;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.common.utils.SegmentLock;
import tech.powerjob.server.common.constants.PJThreadPool;
import tech.powerjob.server.common.utils.OmsFileUtils;
import tech.powerjob.server.extension.dfs.*;
import tech.powerjob.server.persistence.StringPage;
import tech.powerjob.server.persistence.local.LocalInstanceLogDO;
import tech.powerjob.server.persistence.local.LocalInstanceLogRepository;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.storage.Constants;
import tech.powerjob.server.remote.server.redirector.DesignateServer;

import javax.annotation.Resource;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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

    @Value("${server.port}")
    private int port;

    @Resource
    private InstanceMetadataService instanceMetadataService;

    @Resource
    private DFsService dFsService;
    /**
     * 本地数据库操作bean
     */
    @Resource(name = "localTransactionTemplate")
    private TransactionTemplate localTransactionTemplate;

    @Resource
    private LocalInstanceLogRepository localInstanceLogRepository;

    /**
     * 本地维护了在线日志的任务实例ID
     */
    private final Map<Long, Long> instanceId2LastReportTime = Maps.newConcurrentMap();

    @Resource(name = PJThreadPool.BACKGROUND_POOL)
    private AsyncTaskExecutor powerJobBackgroundPool;

    /**
     *  分段锁
     */
    private final SegmentLock segmentLock = new SegmentLock(8);

    /**
     * 格式化时间戳
     */
    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance(OmsConstant.TIME_PATTERN_PLUS);
    /**
     * 每一个展示的行数
     */
    private static final int MAX_LINE_COUNT = 100;
    /**
     * 更新中的日志缓存时间
     */
    private static final long LOG_CACHE_TIME = 10000;

    /**
     * 提交日志记录，持久化到本地数据库中
     * @param workerAddress 上报机器地址
     * @param logs 任务实例运行时日志
     */
    @Async(value = PJThreadPool.LOCAL_DB_POOL)
    public void submitLogs(String workerAddress, List<InstanceLogContent> logs) {

        List<LocalInstanceLogDO> logList = logs.stream().map(x -> {
            instanceId2LastReportTime.put(x.getInstanceId(), System.currentTimeMillis());

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
     * @param appId appId，AOP 专用
     * @param instanceId 任务实例ID
     * @param index 页码，从0开始
     * @return 文本字符串
     */
    @DesignateServer
    public StringPage fetchInstanceLog(Long appId, Long instanceId, Long index) {
        try {
            Future<File> fileFuture = prepareLogFile(instanceId);
            // 超时并不会打断正在执行的任务
            File logFile = fileFuture.get(5, TimeUnit.SECONDS);

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
                log.warn("[InstanceLog-{}] read logFile from disk failed for app: {}.", instanceId, appId, e);
                return StringPage.simple("oms-server execution exception, caused by " + ExceptionUtils.getRootCauseMessage(e));
            }

            double totalPage = Math.ceil(1.0 * lines / MAX_LINE_COUNT);
            return new StringPage(index, (long) totalPage, sb.toString());

        }catch (TimeoutException te) {
            return StringPage.simple("log file is being prepared, please try again later.");
        }catch (Exception e) {
            log.warn("[InstanceLog-{}] fetch instance log failed.", instanceId, e);
            return StringPage.simple("oms-server execution exception, caused by " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    /**
     * 获取日志的下载链接
     * @param appId AOP 专用
     * @param instanceId 任务实例 ID
     * @return 下载链接
     */
    @DesignateServer
    public String fetchDownloadUrl(Long appId, Long instanceId) {
        String url = "http://" + NetUtils.getLocalHost() + ":" + port + "/instance/downloadLog?instanceId=" + instanceId;
        log.info("[InstanceLog-{}] downloadURL for appId[{}]: {}", instanceId, appId, url);
        return url;
    }

    /**
     * 下载全部的任务日志文件
     * @param instanceId 任务实例ID
     * @return 日志文件
     * @throws Exception 异常
     */
    public File downloadInstanceLog(long instanceId) throws Exception {
        Future<File> fileFuture = prepareLogFile(instanceId);
        return fileFuture.get(1, TimeUnit.MINUTES);
    }

    /**
     * 异步准备日志文件
     * @param instanceId 任务实例ID
     * @return 异步结果
     */
    private Future<File> prepareLogFile(long instanceId) {
        return powerJobBackgroundPool.submit(() -> {
            // 在线日志还在不断更新，需要使用本地数据库中的数据
            if (instanceId2LastReportTime.containsKey(instanceId)) {
                return genTemporaryLogFile(instanceId);
            }
            return genStableLogFile(instanceId);
        });
    }

    /**
     * 将本地的任务实例运行日志同步到 mongoDB 存储，在任务执行结束后异步执行
     * @param instanceId 任务实例ID
     */
    @Async(PJThreadPool.BACKGROUND_POOL)
    public void sync(Long instanceId) {

        Stopwatch sw = Stopwatch.createStarted();
        try {
            // 先持久化到本地文件
            File stableLogFile = genStableLogFile(instanceId);
            // 将文件推送到 MongoDB

            FileLocation dfsFL = new FileLocation().setBucket(Constants.LOG_BUCKET).setName(genMongoFileName(instanceId));

            try {
                dFsService.store(new StoreRequest().setLocalFile(stableLogFile).setFileLocation(dfsFL));
                log.info("[InstanceLog-{}] push local instanceLogs to mongoDB succeed, using: {}.", instanceId, sw.stop());
            }catch (Exception e) {
                log.warn("[InstanceLog-{}] push local instanceLogs to mongoDB failed.", instanceId, e);
            }

        }catch (Exception e) {
            log.warn("[InstanceLog-{}] sync local instanceLogs failed.", instanceId, e);
        }
        // 删除本地数据库数据
        try {
            instanceId2LastReportTime.remove(instanceId);
            CommonUtils.executeWithRetry0(() -> localInstanceLogRepository.deleteByInstanceId(instanceId));
            log.info("[InstanceLog-{}] delete local instanceLog successfully.", instanceId);
        }catch (Exception e) {
            log.warn("[InstanceLog-{}] delete local instanceLog failed.", instanceId, e);
        }
    }

    private File genTemporaryLogFile(long instanceId) {
        String path = genLogFilePath(instanceId, false);
        int lockId = ("tpFileLock-" + instanceId).hashCode();
        try {
            segmentLock.lockInterruptibleSafe(lockId);

            // Stream 需要在事务的包裹之下使用
            return localTransactionTemplate.execute(status -> {
                File f = new File(path);
                // 如果文件存在且有效，则不再重新构建日志文件（这个判断也需要放在锁内，否则构建到一半的文件会被返回）
                if (f.exists() && (System.currentTimeMillis() - f.lastModified()) < LOG_CACHE_TIME) {
                    return f;
                }
                try {
                    // 创建父文件夹（文件在开流时自动会被创建）
                    FileUtils.forceMkdirParent(f);

                    // 重新构建文件
                    try (Stream<LocalInstanceLogDO> allLogStream = localInstanceLogRepository.findByInstanceIdOrderByLogTime(instanceId)) {
                        stream2File(allLogStream, f);
                    }
                    return f;
                }catch (Exception e) {
                    CommonUtils.executeIgnoreException(() -> FileUtils.forceDelete(f));
                    throw new RuntimeException(e);
                }
            });
        }finally {
            segmentLock.unlock(lockId);
        }
    }

    private File genStableLogFile(long instanceId) {
        String path = genLogFilePath(instanceId, true);
        int lockId = ("stFileLock-" + instanceId).hashCode();
        try {
            segmentLock.lockInterruptibleSafe(lockId);

            return localTransactionTemplate.execute(status -> {

                File f = new File(path);
                if (f.exists()) {
                    return f;
                }

                try {
                    // 创建父文件夹（文件在开流时自动会被创建）
                    FileUtils.forceMkdirParent(f);

                    // 本地存在数据，从本地持久化（对应 SYNC 的情况）
                    if (instanceId2LastReportTime.containsKey(instanceId)) {
                        try (Stream<LocalInstanceLogDO> allLogStream = localInstanceLogRepository.findByInstanceIdOrderByLogTime(instanceId)) {
                            stream2File(allLogStream, f);
                        }
                    }else {

                        FileLocation dfl = new FileLocation().setBucket(Constants.LOG_BUCKET).setName(genMongoFileName(instanceId));
                        Optional<FileMeta> dflMetaOpt = dFsService.fetchFileMeta(dfl);
                        if (!dflMetaOpt.isPresent()) {
                            OmsFileUtils.string2File("SYSTEM: There is no online log for this job instance.", f);
                            return f;
                        }

                        dFsService.download(new DownloadRequest().setTarget(f).setFileLocation(dfl));
                    }
                    return f;
                }catch (Exception e) {
                    CommonUtils.executeIgnoreException(() -> FileUtils.forceDelete(f));
                    throw new RuntimeException(e);
                }
            });
        }finally {
            segmentLock.unlock(lockId);
        }
    }

    /**
     * 将数据库中存储的日志流转化为磁盘日志文件
     * @param stream 流
     * @param logFile 目标日志文件
     */
    private void stream2File(Stream<LocalInstanceLogDO> stream, File logFile) {
        try (FileWriter fw = new FileWriter(logFile); BufferedWriter bfw = new BufferedWriter(fw)) {
            stream.forEach(instanceLog -> {
                try {
                    bfw.write(convertLog(instanceLog) + System.lineSeparator());
                }catch (Exception ignore) {
                }
            });
        }catch (IOException ie) {
            ExceptionUtils.rethrow(ie);
        }
    }



    /**
     * 拼接日志 -> 2020-04-29 22:07:10.059 [192.168.1.1:2777] INFO XXX
     * @param instanceLog 日志对象
     * @return 字符串
     */
    private static String convertLog(LocalInstanceLogDO instanceLog) {
        return String.format("%s [%s] %s %s",
                DATE_FORMAT.format(instanceLog.getLogTime()),
                instanceLog.getWorkerAddress(),
                LogLevel.genLogLevelString(instanceLog.getLogLevel()),
                instanceLog.getLogContent());
    }


    @Async(PJThreadPool.TIMING_POOL)
    @Scheduled(fixedDelay = 120000)
    public void timingCheck() {

        // 定时删除秒级任务的日志
        List<Long> frequentInstanceIds = Lists.newLinkedList();
        instanceId2LastReportTime.keySet().forEach(instanceId -> {
            try {
                JobInfoDO jobInfo = instanceMetadataService.fetchJobInfoByInstanceId(instanceId);
                if (TimeExpressionType.FREQUENT_TYPES.contains(jobInfo.getTimeExpressionType())) {
                    frequentInstanceIds.add(instanceId);
                }
            }catch (Exception ignore) {
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

        // 删除长时间未 REPORT 的日志（必要性考证中......）
    }


    private static String genLogFilePath(long instanceId, boolean stable) {
        if (stable) {
            return OmsFileUtils.genLogDirPath() + String.format("%d-stable.log", instanceId);
        }else {
            return OmsFileUtils.genLogDirPath() + String.format("%d-temporary.log", instanceId);
        }
    }
    private static String genMongoFileName(long instanceId) {
        return String.format("oms-%d.log", instanceId);
    }

}
