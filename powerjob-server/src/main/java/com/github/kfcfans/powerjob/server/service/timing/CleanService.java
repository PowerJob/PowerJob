package com.github.kfcfans.powerjob.server.service.timing;

import com.github.kfcfans.powerjob.common.InstanceStatus;
import com.github.kfcfans.powerjob.common.WorkflowInstanceStatus;
import com.github.kfcfans.powerjob.server.common.utils.OmsFileUtils;
import com.github.kfcfans.powerjob.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.core.repository.WorkflowInstanceInfoRepository;
import com.github.kfcfans.powerjob.server.persistence.mongodb.GridFsManager;
import com.github.kfcfans.powerjob.server.service.ha.WorkerManagerService;
import com.github.kfcfans.powerjob.server.service.lock.LockService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;

/**
 * CCO（Chief Clean Officer）
 *
 * @author tjq
 * @since 2020/5/18
 */
@Slf4j
@Service
public class CleanService {

    @Resource
    private GridFsManager gridFsManager;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;
    @Resource
    private WorkflowInstanceInfoRepository workflowInstanceInfoRepository;
    @Resource
    private LockService lockService;

    @Value("${oms.instanceinfo.retention}")
    private int instanceInfoRetentionDay;

    @Value("${oms.container.retention.local}")
    private int localContainerRetentionDay;
    @Value("${oms.container.retention.remote}")
    private int remoteContainerRetentionDay;


    private static final int TEMPORARY_RETENTION_DAY = 3;

    // 每天凌晨3点定时清理
    private static final String CLEAN_TIME_EXPRESSION = "0 0 3 * * ?";

    private static final String HISTORY_DELETE_LOCK = "history_delete_lock";


    @Async("omsTimingPool")
    @Scheduled(cron = CLEAN_TIME_EXPRESSION)
    public void timingClean() {

        // 释放本地缓存
        WorkerManagerService.cleanUp();

        // 释放磁盘空间
        cleanLocal(OmsFileUtils.genLogDirPath(), instanceInfoRetentionDay);
        cleanLocal(OmsFileUtils.genContainerJarPath(), localContainerRetentionDay);
        cleanLocal(OmsFileUtils.genTemporaryPath(), TEMPORARY_RETENTION_DAY);

        // 删除数据库历史的数据
        cleanByOneServer();
    }

    /**
     * 只能一台server清理的操作统一到这里执行
     */
    private void cleanByOneServer() {
        // 只要第一个server抢到锁其他server就会返回，所以锁10分钟应该足够了
        boolean lock = lockService.lock(HISTORY_DELETE_LOCK, 10 * 60 * 1000);
        if (!lock) {
            log.info("[CleanService] clean job is already running, just return.");
            return;
        }
        try {
            // 删除数据库运行记录
            cleanInstanceLog();
            cleanWorkflowInstanceLog();
            // 删除 GridFS 过期文件
            cleanRemote(GridFsManager.LOG_BUCKET, instanceInfoRetentionDay);
            cleanRemote(GridFsManager.CONTAINER_BUCKET, remoteContainerRetentionDay);
        } finally {
            lockService.unlock(HISTORY_DELETE_LOCK);
        }
    }

    @VisibleForTesting
    public void cleanLocal(String path, int day) {
        if (day < 0) {
            log.info("[CleanService] won't clean up {} because of offset day <= 0.", path);
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        File dir = new File(path);
        if (!dir.exists()) {
            return;
        }
        File[] logFiles = dir.listFiles();
        if (logFiles == null || logFiles.length == 0) {
            return;
        }

        // 计算最大偏移量
        long maxOffset = day * 24 * 60 * 60 * 1000L;

        for (File f : logFiles) {
            long offset = System.currentTimeMillis() - f.lastModified();
            if (offset >= maxOffset) {
                if (!f.delete()) {
                    log.warn("[CleanService] delete file({}) failed.", f.getName());
                }else {
                    log.info("[CleanService] delete file({}) successfully.", f.getName());
                }
            }
        }
        log.info("[CleanService] clean {} successfully, using {}.", path, stopwatch.stop());
    }

    @VisibleForTesting
    public void cleanRemote(String bucketName, int day) {
        if (day < 0) {
            log.info("[CleanService] won't clean up bucket({}) because of offset day <= 0.", bucketName);
            return;
        }
        if (gridFsManager.available()) {
            Stopwatch stopwatch = Stopwatch.createStarted();
            try {
                gridFsManager.deleteBefore(bucketName, day);
            }catch (Exception e) {
                log.warn("[CleanService] clean remote bucket({}) failed.", bucketName, e);
            }
            log.info("[CleanService] clean remote bucket({}) successfully, using {}.", bucketName, stopwatch.stop());
        }
    }

    @VisibleForTesting
    public void cleanInstanceLog() {
        if (instanceInfoRetentionDay < 0) {
            return;
        }
        try {
            Date t = DateUtils.addDays(new Date(), -instanceInfoRetentionDay);
            int num = instanceInfoRepository.deleteAllByGmtModifiedBeforeAndStatusIn(t, InstanceStatus.finishedStatus);
            log.info("[CleanService] deleted {} instanceInfo records whose modify time before {}.", num, t);
        }catch (Exception e) {
            log.warn("[CleanService] clean instanceInfo failed.", e);
        }
    }

    @VisibleForTesting
    public void cleanWorkflowInstanceLog() {
        if (instanceInfoRetentionDay < 0) {
            return;
        }
        try {
            Date t = DateUtils.addDays(new Date(), -instanceInfoRetentionDay);
            int num = workflowInstanceInfoRepository.deleteAllByGmtModifiedBeforeAndStatusIn(t, WorkflowInstanceStatus.finishedStatus);
            log.info("[CleanService] deleted {} workflow instanceInfo records whose modify time before {}.", num, t);
        }catch (Exception e) {
            log.warn("[CleanService] clean workflow instanceInfo failed.", e);
        }
    }

}
