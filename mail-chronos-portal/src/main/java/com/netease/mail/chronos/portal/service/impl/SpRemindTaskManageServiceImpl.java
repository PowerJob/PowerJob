package com.netease.mail.chronos.portal.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.netease.mail.chronos.base.cron.CronExpression;
import com.netease.mail.chronos.base.enums.BaseStatusEnum;
import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.base.utils.SimpleBeanConvertUtil;
import com.netease.mail.chronos.base.utils.TimeUtil;
import com.netease.mail.chronos.portal.entity.support.SpRemindTaskInfo;
import com.netease.mail.chronos.portal.mapper.support.SpRemindTaskInfoMapper;
import com.netease.mail.chronos.portal.param.RemindTask;
import com.netease.mail.chronos.portal.service.SpRemindTaskManageService;
import com.netease.mail.chronos.portal.util.QueryWrapperUtil;
import com.netease.mail.chronos.portal.vo.RemindTaskVo;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * @author Echo009
 * @since 2021/9/21
 */
@Service
@Slf4j
public class SpRemindTaskManageServiceImpl implements SpRemindTaskManageService {

    private final SpRemindTaskInfoMapper spRemindTaskInfoMapper;

    private final Snowflake snowflake;

    private static final String ORIGIN_ID_COL = "origin_id";

    public SpRemindTaskManageServiceImpl(SpRemindTaskInfoMapper spRemindTaskInfoMapper, @Qualifier("remindTaskIdGenerator") Snowflake snowflake) {
        this.spRemindTaskInfoMapper = spRemindTaskInfoMapper;
        this.snowflake = snowflake;
    }

    /**
     * 创建任务
     *
     * @param task 任务信息
     * @return 创建的任务信息
     */
    @Override
    public RemindTaskVo create(RemindTask task) {
        log.info("[opt:create,message:start,originId:{},detail:{}]", task.getOriginId(), task);
        // 校验 UID , 任务参数
        checkBaseProperties(task);
        val nextTriggerTime = checkCronAndGetNextTriggerTime(task.getCron(),task.getTimeZoneId(), task.getStartTime(), task.getEndTime());

        val spRemindTaskInfo = new SpRemindTaskInfo();
        BeanUtils.copyProperties(task, spRemindTaskInfo);
        val now = new Date();
        spRemindTaskInfo.setCreateTime(now)
                .setUpdateTime(now)
                .setEnable(true)
                .setId(snowflake.nextId())
                .setNextTriggerTime(nextTriggerTime);

        spRemindTaskInfoMapper.insert(spRemindTaskInfo);
        log.info("[opt:create,message:success,originId:{},id:{},detail:{}]", task.getOriginId(), spRemindTaskInfo.getOriginId(), task);
        return SimpleBeanConvertUtil.convert(spRemindTaskInfo,RemindTaskVo.class);
    }

    /**
     * 删除任务（物理删除）
     *
     * @param originId 任务 id
     * @return 被删除的任务信息
     */
    @Override
    public RemindTaskVo delete(String originId) {
        log.info("[opt:delete,message:start,originId:{}]", originId);
        val spRemindTaskInfo = spRemindTaskInfoMapper.selectOne(QueryWrapperUtil.construct(ORIGIN_ID_COL, originId));
        if (spRemindTaskInfo == null) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "删除失败，该任务不存在");
        }
        spRemindTaskInfoMapper.deleteById(spRemindTaskInfo.getId());
        log.info("[opt:delete,message:success,originId:{},id:{},detail:{}]", originId, spRemindTaskInfo.getId(), spRemindTaskInfo);
        return SimpleBeanConvertUtil.convert(spRemindTaskInfo,RemindTaskVo.class);
    }

    /**
     * 更新任务信息
     *
     * @param task 任务信息
     * @return 更新后的任务信息
     */
    @Override
    public RemindTaskVo update(RemindTask task) {
        log.info("[opt:update,message:start,originId:{},detail:{}]", task.getOriginId(), task);
        checkBaseProperties(task);
        val nextTriggerTime = checkCronAndGetNextTriggerTime(task.getCron(),task.getTimeZoneId(), task.getStartTime(), task.getEndTime());
        val spRemindTaskInfo = spRemindTaskInfoMapper.selectOne(QueryWrapperUtil.construct(ORIGIN_ID_COL, task.getOriginId()));
        if (spRemindTaskInfo == null) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "更新失败，该任务不存在");
        }

        BeanUtils.copyProperties(task, spRemindTaskInfo);

        spRemindTaskInfo.setNextTriggerTime(nextTriggerTime)
                .setUpdateTime(new Date());

        spRemindTaskInfoMapper.updateById(spRemindTaskInfo);
        log.info("[opt:update,message:success,originId:{},detail:{}]", task.getOriginId(), task);
        return SimpleBeanConvertUtil.convert(spRemindTaskInfo,RemindTaskVo.class);
    }

    /**
     * 查询任务信息
     *
     * @param originId 原始 id
     * @return 任务信息
     */
    @Override
    public RemindTaskVo query(String originId) {
        val spRemindTaskInfo = spRemindTaskInfoMapper.selectOne(QueryWrapperUtil.construct(ORIGIN_ID_COL, originId));
        if (spRemindTaskInfo == null) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "更新失败，该任务不存在");
        }
        return SimpleBeanConvertUtil.convert(spRemindTaskInfo,RemindTaskVo.class);
    }


    public void checkBaseProperties(RemindTask task) {

        if (StringUtils.isBlank(task.getUid())) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "uid 不能为空");
        }
        if (StringUtils.isBlank(task.getParam())) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "参数信息（param）不能为空");
        }
        // 设置默认的起始时间
        if (task.getStartTime() == null) {
            task.setStartTime(System.currentTimeMillis());
        }

        if (task.getEndTime() != null) {
            if (task.getStartTime() >= task.getEndTime()) {
                throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "任务的起始时间（start_time）必须小于结束时间（end_time）");
            }
            if (task.getEndTime() < System.currentTimeMillis()) {
                throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "任务的结束时间（end_time）必须大于当前时间");
            }
        }
        // 设置次数限制
        if (task.getTimesLimit() == null || task.getTimesLimit() < 0) {
            task.setTimesLimit(0);
        }

    }


    @SneakyThrows
    public long checkCronAndGetNextTriggerTime(String cron,String timeZoneId, Long startTime, Long endTime) {

        // 校验时区是否合法
        val timeZone = TimeUtil.getTimeZoneByZoneId(timeZoneId);

        // 首先校验 cron 是否合法
        val validExpression = CronExpression.isValidExpression(cron);
        if (!validExpression) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "无效的 cron 表达式");
        }
        // 这里一定不会出异常
        val cronExpression = new CronExpression(cron);
        cronExpression.setTimeZone(timeZone);

        val nextValidTimeAfter = cronExpression.getNextValidTimeAfter(new Date(startTime));
        if (nextValidTimeAfter == null) {
            // 不存在下一次调度时间
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "cron 表达式已过期");
        }

        val nextTriggerTime = nextValidTimeAfter.getTime();

        if (endTime != null && nextTriggerTime > endTime) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "cron 表达式已过期，下次调度时间大于调度周期的结束时间");
        }
        return nextTriggerTime;
    }

}
