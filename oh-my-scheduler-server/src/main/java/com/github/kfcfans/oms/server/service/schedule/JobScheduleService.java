package com.github.kfcfans.oms.server.service.schedule;

import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.persistence.repository.JobLogRepository;
import com.github.kfcfans.oms.server.service.ha.WorkerManagerService;
import com.github.kfcfans.oms.server.service.lock.LockService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务调度执行服务
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
@Service
public class JobScheduleService {

    private static final int MAX_BATCH_NUM = 10;

    @Resource
    private LockService lockService;
    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private JobLogRepository jobLogRepository;

    private static final String SCHEDULE_LOCK = "schedule_lock_%d";
    private static final long SCHEDULE_RATE = 10000;

    @Scheduled(fixedRate = SCHEDULE_RATE)
    private void getJob() {
        List<Long> allAppIds = WorkerManagerService.listAppIds();
        if (CollectionUtils.isEmpty(allAppIds)) {
            log.info("[JobScheduleService] current server has no app's job to schedule.");
            return;
        }

        long timeThreshold = System.currentTimeMillis() + 2 * SCHEDULE_RATE;
        Lists.partition(allAppIds, MAX_BATCH_NUM).forEach(partAppIds -> {

            List<String> lockNames = partAppIds.stream().map(JobScheduleService::genLock).collect(Collectors.toList());
            // 1. 先批量获取锁，获取不到就改成单个循环模式
            boolean batchLock = lockService.batchLock(lockNames);
            if (!batchLock) {

            }else {
                try {
                    List<JobInfoDO> jobInfos = jobInfoRepository.findByAppIdInAndNextTriggerTimeLessThanEqual(partAppIds, timeThreshold);

                    // 顺序：先推入进时间轮 -> 写jobLog表 -> 更新nextTriggerTime（原则：宁可重复执行，也不能不调度）


                }catch (Exception e) {

                }
            }
        });
    }

    private static String genLock(Long appId) {
        return String.format(SCHEDULE_LOCK, appId);
    }
}
