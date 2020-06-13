package com.github.kfcfans.powerjob.server.service.alarm;

import com.github.kfcfans.powerjob.server.persistence.core.model.UserInfoDO;

import java.util.List;

/**
 * 报警接口
 *
 * @author tjq
 * @since 2020/4/19
 */
public interface Alarmable {

    /**
     * 任务执行失败报警
     * @param content 任务实例相关信息
     * @param targetUserList 目标用户列表
     */
    void onJobInstanceFailed(JobInstanceAlarmContent content, List<UserInfoDO> targetUserList);

    /**
     * 工作流执行失败报警
     * @param content 工作流实例相关信息
     * @param targetUserList 目标用户列表
     */
    void onWorkflowInstanceFailed(WorkflowInstanceAlarmContent content, List<UserInfoDO> targetUserList);
}
