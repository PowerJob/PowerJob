package com.netease.mail.chronos.executor.support.service;

import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
import com.netease.mail.chronos.executor.support.entity.SpRtTaskInstance;
import tech.powerjob.worker.log.OmsLogger;

/**
 * @author Echo009
 * @since 2021/10/21
 */
public interface NotifyService {


    /**
     * 发送通知
     * @param spRtTaskInstance 提醒任务
     * @param omsLogger logger
     */
    boolean sendNotify(SpRtTaskInstance spRtTaskInstance, OmsLogger omsLogger);


}
