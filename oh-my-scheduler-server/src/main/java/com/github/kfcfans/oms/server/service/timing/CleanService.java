package com.github.kfcfans.oms.server.service.timing;

import com.github.kfcfans.oms.server.common.utils.OmsFileUtils;
import com.github.kfcfans.oms.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.oms.server.persistence.mongodb.GridFsManager;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
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

    @Value("${oms.log.retention.local}")
    private int localLogRetentionDay;
    @Value("${oms.log.retention.remote}")
    private int remoteLogRetentionDay;
    @Value("${oms.container.retention.local}")
    private int localContainerRetentionDay;
    @Value("${oms.container.retention.remote}")
    private int remoteContainerRetentionDay;

    @Value("${oms.instanceinfo.retention}")
    private int instanceInfoRetentionDay;

    private static final int TEMPORARY_RETENTION_DAY = 3;

    // 每天凌晨3点定时清理
    private static final String CLEAN_TIME_EXPRESSION = "0 0 3 * * ?";


    @Async("omsTimingPool")
    @Scheduled(cron = CLEAN_TIME_EXPRESSION)
    public void timingClean() {

        WorkerManagerService.releaseContainerInfos();

        cleanInstanceLog();

        cleanLocal(OmsFileUtils.genLogDirPath(), localLogRetentionDay);
        cleanLocal(OmsFileUtils.genContainerJarPath(), localContainerRetentionDay);
        cleanLocal(OmsFileUtils.genTemporaryPath(), TEMPORARY_RETENTION_DAY);

        cleanRemote(GridFsManager.LOG_BUCKET, remoteLogRetentionDay);
        cleanRemote(GridFsManager.CONTAINER_BUCKET, remoteContainerRetentionDay);
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
        long maxOffset = day * 24 * 60 * 60 * 1000;

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
        try {
            Date t = DateUtils.addDays(new Date(), -instanceInfoRetentionDay);
            int num = instanceInfoRepository.deleteAllByGmtModifiedBefore(t);
            log.info("[CleanService] deleted {} instanceInfo records whose modify time before {}.", num, t);
        }catch (Exception e) {
            log.warn("[CleanService] clean instanceInfo failed.", e);
        }
    }

}
