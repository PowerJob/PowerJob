package com.netease.mail.chronos.executor.support.service.auxiliary.impl;

import com.netease.mail.chronos.executor.support.base.mapper.TaskInstanceBaseMapper;
import com.netease.mail.chronos.executor.support.entity.SpRtTaskInstance;
import com.netease.mail.chronos.executor.support.enums.TaskInstanceHandleStrategy;
import com.netease.mail.chronos.executor.support.mapper.SpRtTaskInstanceMapper;
import com.netease.mail.chronos.executor.support.service.auxiliary.AbstractTaskInstanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;


/**
 * @author Echo009
 * @since 2021/10/26
 *
 * 提醒任务实例
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpTaskInstanceHandleServiceImpl extends AbstractTaskInstanceService<SpRtTaskInstance> {

    private final SpRtTaskInstanceMapper spRtTaskInstanceMapper;

    @Override
    public long getThresholdDelta() {
        // 30 s
        return 30000;
    }

    @Override
    public int getScope() {
        return 2;
    }

    @Override
    public TaskInstanceHandleStrategy matchStrategy() {
        return TaskInstanceHandleStrategy.REMIND_TASK;
    }

    @Override
    public TaskInstanceBaseMapper<SpRtTaskInstance> getMapper() {
        return spRtTaskInstanceMapper;
    }
}
