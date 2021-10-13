package com.netease.mail.chronos.portal.service;

import com.netease.mail.chronos.portal.param.RemindTask;
import com.netease.mail.chronos.portal.vo.RemindTaskVo;

import java.util.List;

/**
 * @author Echo009
 * @since 2021/9/21
 */
public interface SpRemindTaskManageService {

    /**
     * 创建任务
     *
     * @param task 任务信息
     * @return 创建的任务信息
     */
    List<RemindTaskVo> create(RemindTask task);

    /**
     * 删除任务（物理删除）
     *
     * @param colId  colId
     * @param compId compId
     * @return 被删除的任务信息
     */
    List<RemindTaskVo> delete(String colId, String compId);

    /**
     * 更新任务信息
     *
     * @param task 任务信息
     * @return 更新后的任务信息
     */
    List<RemindTaskVo> update(RemindTask task);


    /**
     * 查询任务信息
     *
     * @param colId  colId
     * @param compId compId
     * @return 任务信息
     */
    List<RemindTaskVo> query(String colId, String compId);


}
