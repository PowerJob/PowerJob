package com.netease.mail.chronos.executor.support.base.mapper;

import com.baomidou.mybatisplus.core.mapper.Mapper;
import com.netease.mail.chronos.executor.support.base.po.TaskInstancePrimaryKey;
import com.netease.mail.chronos.executor.support.entity.base.TaskInstance;

import java.util.List;

/**
 * @author Echo009
 * @since 2021/10/28
 */
public interface TaskInstanceBaseMapper<T extends TaskInstance> extends Mapper<T> {


    /**
     * 获取需要触发的实例列表
     *
     * @param threshold 触发阈值（在此时间点之前的）
     * @return 需要触发的任务实例
     */
    List<TaskInstancePrimaryKey> selectIdListOfNeedTriggerInstance(Long threshold, List<Integer> partitionKeyList);

    /**
     * 插入任务实例
     *
     * @param taskInstance 任务实例
     * @return count
     */
    int insert(T taskInstance);

    /**
     * 根据主键查询
     *
     * @param id           id
     * @param partitionKey 分区键
     * @return TaskInstance
     */
    T selectByPrimaryKey(Long id, Integer partitionKey);

    /**
     * 根据主键更新记录
     *
     * @param taskInstance 任务实例
     * @return 更新数量
     */
    int updateByPrimaryKey(T taskInstance);


}
