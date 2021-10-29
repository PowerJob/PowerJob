package com.netease.mail.chronos.executor.support.service.auxiliary;

import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.base.utils.ExecuteUtil;
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

    private static final String PARTITION_PREFIX = "p";

    /**
     * 获取触发阈值差值
     */
    public abstract long getThresholdDelta();

    /**
     * 获取查询范围
     * 比如
     * 1 ，只处理当天的任务
     * 2 ，只处理当天以及前一天的任务
     * <p>
     * 一般情况下，应该只使用 2
     */
    public abstract int getScope();

    /**
     * 返回匹配的策略信息
     *
     * @return TaskInstanceHandleStrategy
     */
    public abstract TaskInstanceHandleStrategy matchStrategy();

    /**
     * 加载需要处理的任务实例 id 列表
     *
     * @return id 列表
     */
    public List<TaskInstancePrimaryKey> loadHandleInstanceIdList(int limit) {
        TaskInstanceBaseMapper<T> mapper = getMapper();
        // cal
        long triggerThreshold = System.currentTimeMillis() + getThresholdDelta();
        int scope = getScope();
        log.info("load task instance,triggerThreshold:{},limit:{},scope:{}", triggerThreshold, limit, scope);
        return mapper.selectIdListOfNeedTriggerInstance(triggerThreshold, obtainPartitionKeyListByScope(scope), limit);
    }

    public T selectByPrimaryKey(TaskInstancePrimaryKey primaryKey) {
        return getMapper().selectByPrimaryKey(primaryKey.getId(), primaryKey.getPartitionKey());
    }


    public void updatePartition() {

        TaskInstanceBaseMapper<T> mapper = getMapper();
        // 删除 7 天之前的分区
        Date dropDate = TimeUtil.obtainNextNDay(new Date(), -7);
        Integer dateNumber = TimeUtil.getDateNumber(dropDate);
        String dropP = PARTITION_PREFIX + dateNumber;
        log.info("drop partition:{}",dropP);
        ExecuteUtil.executeIgnoreExceptionWithoutReturn(() -> mapper.dropPartition(dropP));
        // 创建第二天的分区
        Date createDate = TimeUtil.obtainNextNDay(new Date(), 2);
        dateNumber = TimeUtil.getDateNumber(createDate);
        Integer valueLimit = dateNumber + 1;
        String createP = PARTITION_PREFIX + dateNumber;
        log.info("create partition:{},valueLimit:{}",createP,valueLimit);
        ExecuteUtil.executeIgnoreExceptionWithoutReturn(() -> mapper.createPartition(createP,valueLimit));

    }


    public void updateByPrimaryKey(T taskInstance) {
        checkPrimaryKey(taskInstance);
        getMapper().updateByPrimaryKey(taskInstance);
    }


    public void insert(T taskInstance) {
        checkPrimaryKey(taskInstance);
        getMapper().insert(taskInstance);
    }

    private void checkPrimaryKey(T taskInstance) {
        if (taskInstance.getId() == null || taskInstance.getPartitionKey() == null) {
            throw new BaseException("任务实例 ID 以及 分区键不能为空！");
        }
    }

    private List<Integer> obtainPartitionKeyListByScope(int scope) {
        Date start = TimeUtil.obtainCurrentDate();
        List<Integer> res = new ArrayList<>(scope);
        for (int i = 0; i < scope; i++) {
            res.add(TimeUtil.getDateNumber(TimeUtil.obtainNextNDay(start, -i)));
        }
        return res;
    }


    public abstract TaskInstanceBaseMapper<T> getMapper();

}
