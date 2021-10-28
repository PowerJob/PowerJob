package com.netease.mail.chronos.executor.support.service.auxiliary;

import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.base.utils.TimeUtil;
import com.netease.mail.chronos.executor.support.base.mapper.TaskInstanceBaseMapper;
import com.netease.mail.chronos.executor.support.base.po.TaskInstancePrimaryKey;
import com.netease.mail.chronos.executor.support.entity.base.TaskInstance;
import com.netease.mail.chronos.executor.support.enums.TaskInstanceHandleStrategy;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Echo009
 * @since 2021/10/26
 */
@Slf4j
public abstract class AbstractTaskInstanceService<T extends TaskInstance> {

    /**
     * 获取触发阈值差值
     */
    abstract long getThresholdDelta();

    /**
     * 获取查询范围
     * 比如
     * 1 ，只处理当天的任务
     * 2 ，只处理当天以及前一天的任务
     *
     * 一般情况下，应该只使用 2
     */
    abstract int getScope();
    /**
     * 返回匹配的策略信息
     *
     * @return TaskInstanceHandleStrategy
     */
    abstract TaskInstanceHandleStrategy matchStrategy();

    /**
     * 加载需要处理的任务实例 id 列表
     *
     * @return id 列表
     */
    public List<TaskInstancePrimaryKey> loadHandleInstanceIdList() {
        TaskInstanceBaseMapper<T> mapper = getMapper();
        // cal
        long triggerThreshold = System.currentTimeMillis() + getThresholdDelta();
        int scope = getScope();
        return mapper.selectIdListOfNeedTriggerInstance(triggerThreshold,obtainPartitionKeyListByScope(scope));
    }


    public int updateByPrimaryKey(T taskInstance){
        if (taskInstance.getId() == null || taskInstance.getPartitionKey() == null) {
            throw new BaseException("任务实例 ID 以及 分区键不能为空！");
        }
        // todo
        return taskInstance.getMaxRetryTimes();
    }

    private List<Integer> obtainPartitionKeyListByScope(int scope){
        Date start = TimeUtil.obtainCurrentDate();
        List<Integer> res = new ArrayList<>(scope);
        for (int i = 0; i < scope; i++) {
            res.add(TimeUtil.getDateNumber(TimeUtil.obtainNextNDay(start, -i)));
        }
        return res;
    }



    abstract TaskInstanceBaseMapper<T> getMapper();

}
