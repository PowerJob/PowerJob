package com.netease.mail.chronos.executor.support.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.netease.mail.chronos.base.utils.TimeUtil;
import com.netease.mail.chronos.executor.support.entity.SpRemindTaskInfo;
import com.netease.mail.chronos.executor.support.mapper.SpRemindTaskInfoMapper;
import com.netease.mail.chronos.executor.support.po.SpRemindTaskSimpleInfo;
import com.netease.mail.chronos.executor.support.service.SpRemindTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Date;
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
                .map(SpRemindTaskSimpleInfo::getId)
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

    @Override
    public List<SpRemindTaskInfo> obtainOutOfDateDisableTask() {
        Date b15 = TimeUtil.obtainNextNDay(new Date(), -15);
        QueryWrapper<SpRemindTaskInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("enable", false).le("disable_time", b15);
        return spRemindTaskInfoMapper.selectList(wrapper);
    }

    @Override
    public List<SpRemindTaskInfo> obtainStagnantTask() {
        // 超过 5 分钟没有触发的，且未被 disable
        long threshold = System.currentTimeMillis() - 300_000L;
        QueryWrapper<SpRemindTaskInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("enable", true).le("next_trigger_time", threshold);
        return spRemindTaskInfoMapper.selectList(wrapper);
    }

    @Override
    public void deleteById(Long id) {
        spRemindTaskInfoMapper.deleteById(id);
    }
}
