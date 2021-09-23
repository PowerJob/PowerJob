package com.netease.mail.chronos.portal.service;

import com.netease.mail.chronos.portal.param.RemindTask;
import com.netease.mail.chronos.portal.vo.RemindTaskVo;

/**
 * @author Echo009
 * @since 2021/9/21
 */
public interface SpRemindTaskManageService {

    /**
     * 创建任务
     * @param task 任务信息
     * @return 创建的任务信息
     */
    RemindTaskVo create(RemindTask task);

    /**
     * 删除任务（物理删除）
     * @param originId 任务 id
     * @return 被删除的任务信息
     */
    RemindTaskVo delete(String originId);

    /**
     * 更新任务信息
     * @param task 任务信息
     * @return 更新后的任务信息
     */
    RemindTaskVo update(RemindTask task);


    /**
     * 查询任务信息
     * @param originId 原始 id
     * @return 任务信息
     */
    RemindTaskVo query(String originId);


}
