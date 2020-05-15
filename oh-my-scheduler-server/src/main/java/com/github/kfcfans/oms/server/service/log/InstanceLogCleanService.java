package com.github.kfcfans.oms.server.service.log;

import com.github.kfcfans.oms.server.common.utils.OmsFileUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Stopwatch;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;

/**
 * 定时清理任务实例运行日志，包括本地和MongoDB
 *
 * @author tjq
 * @since 2020/5/11
 */
@Slf4j
@Service
public class InstanceLogCleanService {

    @Value("${oms.log.retention.local}")
    private int localRetentionDay;
    @Value("${oms.log.retention.remote}")
    private int remoteRetentionDay;

    // 直接操作 mongoDB 文件系统
    private GridFsTemplate gridFsTemplate;
    // 每天凌晨3点定时清理
    private static final String CLEAN_TIME_EXPRESSION = "0 0 3 * * ?";

    @Async("omsTimingPool")
    @Scheduled(cron = CLEAN_TIME_EXPRESSION)
    public void timingClean() {
        cleanRemote();
        cleanLocal();
    }

    @VisibleForTesting
    public void cleanLocal() {

        if (localRetentionDay < 0) {
            log.info("[InstanceLogCleanService] won't clean up local logs because of localRetentionDay <= 0.");
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        String path = OmsFileUtils.genLogDirPath();
        File dir = new File(path);
        if (!dir.exists()) {
            return;
        }
        File[] logFiles = dir.listFiles();
        if (logFiles == null || logFiles.length == 0) {
            return;
        }

        // 计算最大偏移量
        long maxOffset = localRetentionDay * 24 * 60 * 60 * 1000;

        for (File f : logFiles) {
            long offset = System.currentTimeMillis() - f.lastModified();
            if (offset >= maxOffset) {
                if (!f.delete()) {
                    log.warn("[InstanceLogCleanService] delete local log({}) failed.", f.getName());
                }else {
                    log.info("[InstanceLogCleanService] delete local log({}) successfully.", f.getName());
                }
            }
        }
        log.info("[InstanceLogCleanService] clean local instance log file successfully, using {}.", stopwatch.stop());
    }

    @VisibleForTesting
    public void cleanRemote() {

        if (remoteRetentionDay < 0) {
            log.info("[InstanceLogCleanService] won't clean up remote logs because of remoteRetentionDay <= 0.");
            return;
        }
        if (gridFsTemplate == null) {
            return;
        }

        Stopwatch stopwatch = Stopwatch.createStarted();
        try {
            // 计算时间
            Date date = DateUtils.addDays(new Date(), -remoteRetentionDay);
            Query mongoQuery = Query.query(Criteria.where("uploadDate").lt(date));
            gridFsTemplate.delete(mongoQuery);
        }catch (Exception e) {
            log.warn("[InstanceLogCleanService] clean remote log failed.", e);
        }
        log.info("[InstanceLogCleanService] clean remote instance log file finished, using {}.", stopwatch.stop());
    }

    @Autowired(required = false)
    public void setGridFsTemplate(GridFsTemplate gridFsTemplate) {
        this.gridFsTemplate = gridFsTemplate;
    }

}
