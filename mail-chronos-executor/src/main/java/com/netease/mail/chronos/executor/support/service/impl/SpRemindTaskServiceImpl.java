package com.netease.mail.chronos.executor.support.service.impl;

import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
import com.netease.mail.chronos.executor.support.mapper.SpRemindTaskInfoMapper;
import com.netease.mail.chronos.executor.support.po.SpRemindTaskSimpleInfo;
import com.netease.mail.chronos.executor.support.service.SpRemindTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Echo009
 * @since 2021/9/27
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SpRemindTaskServiceImpl implements SpRemindTaskService {

    private final SpRemindTaskInfoMapper spRemindTaskInfoMapper;


    @Override
    public List<Long> obtainValidTaskIdListByTriggerTimeScope(long minTriggerTime, long maxTriggerTime) {
        // 拉取小于 maxTriggerTime 的任务
        List<SpRemindTaskSimpleInfo> spRemindTaskInfos = spRemindTaskInfoMapper.selectIdListByNextTriggerTimeAndEnable(maxTriggerTime);
        // 过滤小于 minTriggerTime
        if (spRemindTaskInfos == null || spRemindTaskInfos.isEmpty()) {
            return Collections.emptyList();
        }
        return spRemindTaskInfos.stream()
                .filter(e -> e.getNextTriggerTime() != null && e.getNextTriggerTime() >= minTriggerTime)
                .map(SpRemindTaskSimpleInfo::getNextTriggerTime)
                .collect(Collectors.toList());
    }






    @Override
    public SpRemindTaskInfo selectById(long id) {
        return spRemindTaskInfoMapper.selectById(id);
    }

    @Override
    public int updateById(SpRemindTaskInfo spRemindTaskInfo) {
       return spRemindTaskInfoMapper.updateById(spRemindTaskInfo);
    }
}
