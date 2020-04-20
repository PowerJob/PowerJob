package com.github.kfcfans.oms.server.service.alarm;

import com.github.kfcfans.oms.server.persistence.model.InstanceLogDO;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;

/**
 * 报警接口
 *
 * @author tjq
 * @since 2020/4/19
 */
public interface Alarmable {

    void alarm(JobInfoDO jobInfo, InstanceLogDO instanceLog);
}
