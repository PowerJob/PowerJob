package com.github.kfcfans.oms.server.service.alarm;

import com.github.kfcfans.oms.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.JobInfoDO;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 报警服务
 *
 * @author tjq
 * @since 2020/4/19
 */
@Slf4j
public class AlarmService {

    private static List<Alarmable> alarmableList = Lists.newLinkedList();

    public static void alarm(JobInfoDO jobInfo, InstanceInfoDO instanceLog) {
        if (CollectionUtils.isEmpty(alarmableList)) {
            return;
        }
        alarmableList.forEach(alarmable -> {
            try {
                alarmable.alarm(jobInfo, instanceLog);
            }catch (Exception e) {
                log.warn("[AlarmService] alarm failed.", e);
            }
        });
    }
}
