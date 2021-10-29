package com.netease.mail.chronos.executor.support.mapper;

import com.netease.mail.chronos.executor.support.base.mapper.TaskInstanceBaseMapper;
import com.netease.mail.chronos.executor.support.base.po.TaskInstancePrimaryKey;
import com.netease.mail.chronos.executor.support.entity.SpRtTaskInstance;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author  com.netease.mail.chronos.executor.support.entity.SpRtTaskInstance
 */
public interface SpRtTaskInstanceMapper extends TaskInstanceBaseMapper<SpRtTaskInstance> {
    /**
     * 加载需要被处理的任务
     * triggerTime < threshold
     * enable = 1
     * status ！= 3
     */
    @Override
    List<TaskInstancePrimaryKey> selectIdListOfNeedTriggerInstance(@Param("threshold") Long threshold, @Param("partitionKeyList") List<Integer> partitionKeyList,@Param("limit") int limit);

    @Override
    SpRtTaskInstance selectByPrimaryKey(@Param("id") Long id, @Param("partitionKey") Integer partitionKey);

    @Override
    int updateByPrimaryKey(@Param("taskInstance") SpRtTaskInstance taskInstance);

    /**
     * ! 这里会忽略唯一索引重复的异常
     */
    @Override
    int insert(@Param("taskInstance") SpRtTaskInstance taskInstance);

    @Override
    void createPartition(@Param("partitionName") String partitionName, @Param("valueLimit") Integer valueLimit);

    @Override
    void dropPartition(@Param("partitionName") String partitionName);
}




