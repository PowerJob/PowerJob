package com.netease.mail.chronos.executor.support.service;

import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;

import java.util.List;

/**
 * @author Echo009
 * @since 2021/9/27
 */
public interface SpRemindTaskService {

    /**
     * 根据触发时间范围获取有效的任务 id 列表
     *
     * @param minTriggerTime 最小触发时间
     * @param maxTriggerTime 最大触发时间
     * @return id list
     */
    List<Long> obtainValidTaskIdListByTriggerTimeScope(long minTriggerTime, long maxTriggerTime);

    /**
     * 根据 ID 查找记录
     * @param id 任务 id
     * @return 提醒任务详情
     */
    SpRemindTaskInfo selectById(long id);

    /**
     * 根据 ID 更新记录
     * @param spRemindTaskInfo 提醒任务详情
     * @return 更新记录数
     */
    int updateById(SpRemindTaskInfo spRemindTaskInfo);

    /**
     * 获取需要被清理的任务
     */
    List<SpRemindTaskInfo> obtainOutOfDateDisableTask();

    /**
     * 获取停滞的任务
     * trigger time 已过期，且没有被 disable
     */
    List<SpRemindTaskInfo> obtainStagnantTask();


    void deleteById(Long id);

}
