package com.netease.mail.chronos.portal.service.impl;

import cn.hutool.core.lang.Snowflake;
import com.google.common.collect.Maps;
import com.netease.mail.chronos.base.enums.BaseStatusEnum;
import com.netease.mail.chronos.base.exception.BaseException;
import com.netease.mail.chronos.base.utils.ICalendarRecurrenceRuleUtil;
import com.netease.mail.chronos.base.utils.SimpleBeanConvertUtil;
import com.netease.mail.chronos.base.utils.TimeUtil;
import com.netease.mail.chronos.portal.entity.support.SpRemindTaskInfo;
import com.netease.mail.chronos.portal.enums.RemindTaskApiStatusEnum;
import com.netease.mail.chronos.portal.mapper.support.SpRemindTaskInfoMapper;
import com.netease.mail.chronos.portal.param.RemindTask;
import com.netease.mail.chronos.portal.service.SpRemindTaskManageService;
import com.netease.mail.chronos.portal.util.QueryWrapperUtil;
import com.netease.mail.chronos.portal.vo.RemindTaskVo;
import com.netease.mail.quark.commons.serialization.JacksonUtils;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import net.fortuna.ical4j.model.Recur;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Echo009
 * @since 2021/9/21
 */
@Service
@Slf4j
public class SpRemindTaskManageServiceImpl implements SpRemindTaskManageService {

    private final SpRemindTaskInfoMapper spRemindTaskInfoMapper;

    private final Snowflake snowflake;

    private static final String P_COL_NAME = "col_id";
    private static final String S_COL_NAME = "comp_id";

    private static final Long MIN_INTERVAL = 60_000L;

    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public SpRemindTaskManageServiceImpl(SpRemindTaskInfoMapper spRemindTaskInfoMapper, @Qualifier("remindTaskIdGenerator") Snowflake snowflake) {
        this.spRemindTaskInfoMapper = spRemindTaskInfoMapper;
        this.snowflake = snowflake;
    }

    /**
     * 创建任务
     */
    @Override
    @Transactional(value = "chronosSupportTransactionManager", rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public List<RemindTaskVo> create(RemindTask task) {
        log.info("[opt:create,message:start,colId:{},compId:{},detail:{}]", task.getColId(), task.getCompId(), task);
        // 校验 UID , 任务参数
        checkBaseProperties(task);
        // 校验是否重复
        List<SpRemindTaskInfo> origin = spRemindTaskInfoMapper.selectList(QueryWrapperUtil.construct(P_COL_NAME, task.getColId(), S_COL_NAME, task.getCompId()));
        if (origin != null) {
            throw new BaseException(RemindTaskApiStatusEnum.ALREADY_EXISTS);
        }
        List<Long> triggerOffsets = task.getTriggerOffsets();
        List<RemindTaskVo> res = new ArrayList<>(triggerOffsets.size());
        val now = new Date();
        for (Long triggerOffset : triggerOffsets) {
            SpRemindTaskInfo spRemindTaskInfo = constructSpRemindTaskInfo(task, now, triggerOffset);
            spRemindTaskInfoMapper.insert(spRemindTaskInfo);
            res.add(SimpleBeanConvertUtil.convert(spRemindTaskInfo, RemindTaskVo.class));
            log.info("[opt:create,message:success,colId:{},compId:{},id:{},detail:{}]", task.getColId(), task.getCompId(), spRemindTaskInfo.getId(), task);
        }
        return res;
    }

    /**
     * 删除任务（物理删除）
     */
    @Override
    @Transactional(value = "chronosSupportTransactionManager", rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public List<RemindTaskVo> delete(String colId, String compId) {
        log.info("[opt:delete,message:start,col:{},compId:{}]", colId, compId);
        List<SpRemindTaskInfo> deleteList;
        if (StringUtils.isNotBlank(compId)) {
            // 根据 compId 删除
            deleteList = spRemindTaskInfoMapper.selectList(QueryWrapperUtil.construct(S_COL_NAME, compId));
        } else {
            deleteList = spRemindTaskInfoMapper.selectList(QueryWrapperUtil.construct(P_COL_NAME, colId));
        }

        if (CollectionUtils.isEmpty(deleteList)) {
            log.warn("[opt:delete,message:failed,task is not exist,col:{},compId:{}]", colId, compId);
            return Collections.emptyList();
        }
        spRemindTaskInfoMapper.deleteBatchIds(deleteList.stream().map(SpRemindTaskInfo::getId).collect(Collectors.toList()));
        ArrayList<RemindTaskVo> res = new ArrayList<>(deleteList.size());
        for (SpRemindTaskInfo spRemindTaskInfo : deleteList) {
            res.add(SimpleBeanConvertUtil.convert(spRemindTaskInfo, RemindTaskVo.class));
        }
        log.info("[opt:delete,message:success,col:{},compId:{},list:{}]", colId, compId, deleteList);
        return res;
    }

    /**
     * 更新任务信息
     */
    @Override
    @Transactional(value = "chronosSupportTransactionManager", rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public List<RemindTaskVo> update(RemindTask task) {
        log.info("[opt:update,message:start,colId:{},compId:{},detail:{}]", task.getColId(), task.getCompId(), task);
        // 校验 UID , 任务参数
        checkBaseProperties(task);
        // 先 select 再删除
        List<Long> ids = spRemindTaskInfoMapper.selectIdListByCompId(task.getCompId());
        if (!CollectionUtils.isEmpty(ids)) {
            int deleted = spRemindTaskInfoMapper.deleteBatchIds(ids);
            log.info("[opt:update,message:delete origin task info success,colId:{},compId:{},count:{}]", task.getColId(), task.getCompId(), deleted);
        }
        // 创建
        List<Long> triggerOffsets = task.getTriggerOffsets();
        List<RemindTaskVo> res = new ArrayList<>(triggerOffsets.size());
        val now = new Date();
        for (Long triggerOffset : triggerOffsets) {
            SpRemindTaskInfo spRemindTaskInfo = constructSpRemindTaskInfo(task, now, triggerOffset);
            spRemindTaskInfoMapper.insert(spRemindTaskInfo);
            res.add(SimpleBeanConvertUtil.convert(spRemindTaskInfo, RemindTaskVo.class));
            log.info("[opt:update,message:create task ,colId:{},compId:{},id:{},detail:{}]", task.getColId(), task.getCompId(), spRemindTaskInfo.getId(), task);
        }
        return res;
    }

    private SpRemindTaskInfo constructSpRemindTaskInfo(RemindTask task, Date now, Long triggerOffset) {
        val nextTriggerTime = calNextTriggerTime(task, triggerOffset);
        val spRemindTaskInfo = new SpRemindTaskInfo();
        HashMap<String, String> extra = Maps.newHashMap();
        spRemindTaskInfo.setColId(task.getColId())
                .setCompId(task.getCompId())
                .setUid(task.getUid())
                .setRecurrenceRule(task.getRecurrenceRule())
                .setStartTime(task.getSeedTime())
                .setTriggerOffset(triggerOffset)
                .setTimeZoneId(task.getTimeZoneId())
                .setParam(task.getParam())
                // 解析出来的信息
                .setTimesLimit(task.getTimesLimit())
                .setEndTime(task.getEndTime())
                // 分配 or 计算得到的信息
                .setNextTriggerTime(nextTriggerTime)
                .setCreateTime(now)
                .setUpdateTime(now)
                .setEnable(true)
                .setId(snowflake.nextId())
                .setNextTriggerTime(nextTriggerTime);
        handleIllegalTask(spRemindTaskInfo);
        // locale
        if(task.getLocale() == null){
            task.setLocale(Locale.CHINA);
        }
        extra.put("locale",task.getLocale().toString());
        spRemindTaskInfo.setExtra(JacksonUtils.toString(extra));
        return spRemindTaskInfo;
    }

    /**
     * 查询任务信息
     */
    @Override
    public List<RemindTaskVo> query(String colId, String compId) {
        List<SpRemindTaskInfo> list;
        if (StringUtils.isNotBlank(compId)) {
            // 根据 compId 查询
            list = spRemindTaskInfoMapper.selectList(QueryWrapperUtil.construct(S_COL_NAME, compId));
        } else {
            list = spRemindTaskInfoMapper.selectList(QueryWrapperUtil.construct(P_COL_NAME, colId));
        }

        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        List<RemindTaskVo> res = new ArrayList<>(list.size());
        for (SpRemindTaskInfo spRemindTaskInfo : list) {
            res.add(SimpleBeanConvertUtil.convert(spRemindTaskInfo, RemindTaskVo.class));
        }
        return res;
    }

    private void handleIllegalTask(SpRemindTaskInfo task) {

        if (task.getNextTriggerTime() == 0) {
            task.setEnable(false)
                    .setDisableTime(new Date());
            return;
        }
        if (StringUtils.isBlank(task.getRecurrenceRule()) && task.getNextTriggerTime() < System.currentTimeMillis() + MIN_INTERVAL) {
            task.setEnable(false)
                    .setDisableTime(new Date());
            return;
        }
        // 判断结束时间
        if (task.getEndTime() != null) {
            if (task.getEndTime() >= task.getStartTime() && task.getNextTriggerTime() <= task.getEndTime()) {
                return;
            }
            // 结束时间小于 开始时间 ，或者小于 下次触发时间
            task.setEnable(false)
                    .setDisableTime(new Date());
        }
    }

    private void checkBaseProperties(RemindTask task) {

        if (StringUtils.isBlank(task.getColId())) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "colId 不能为空");
        }
        if (StringUtils.isBlank(task.getCompId())) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "compId 不能为空");
        }
        if (StringUtils.isBlank(task.getUid())) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "uid 不能为空");
        }
        if (StringUtils.isBlank(task.getParam())) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "参数信息（param）不能为空");
        }
        // 设置默认的起始时间
        if (task.getSeedTime() == null) {
            throw new BaseException(BaseStatusEnum.ILLEGAL_ARGUMENT.getCode(), "任务的种子时间（seedTime）不能为空");
        }
        task.setStartTime(task.getSeedTime());
        // 校验时区是否合法
        TimeUtil.getTimeZoneByZoneId(task.getTimeZoneId());
        // 解析 重复规则
        parseRecurrenceRule(task);
        // 设置次数限制
        if (task.getTimesLimit() == null || task.getTimesLimit() < 0) {
            task.setTimesLimit(0);
        }
        // 检查 triggerOffsets
        if (task.getTriggerOffsets() == null || task.getTriggerOffsets().isEmpty()) {
            task.setTriggerOffsets(Collections.singletonList(0L));
        }

    }


    private void parseRecurrenceRule(RemindTask task) {

        if (StringUtils.isBlank(task.getRecurrenceRule())) {
            return;
        }
        //
        Recur recur = ICalendarRecurrenceRuleUtil.construct(task.getRecurrenceRule());
        net.fortuna.ical4j.model.Date until = recur.getUntil();
        // 结束时间
        if (until != null) {
            task.setEndTime(until.getTime());
        }
        // 次数限制
        int count = recur.getCount();
        task.setTimesLimit(count);


    }


    private long calNextTriggerTime(RemindTask task, long triggerOffset) {
        val now = System.currentTimeMillis();
        if (StringUtils.isBlank(task.getRecurrenceRule())) {
            long next = task.getSeedTime() + triggerOffset;
            if (now + MIN_INTERVAL >= next) {
                log.warn("[opt:calNextTriggerTime,message:current task(colId:{},compId:{})'s nextTriggerTime < currentTime + 60s,maybe misfire]", task.getColId(), task.getCompId());
            }
            return next;
        }
        long nextValidTimeAfter = ICalendarRecurrenceRuleUtil.calculateNextTriggerTime(task.getRecurrenceRule(), task.getStartTime() + triggerOffset, now);
        // 等于 0 表示不存在下一次触发时间
        if (nextValidTimeAfter == 0L) {
            // 不存在下一次调度时间
            log.warn("[opt:calNextTriggerTime,message:current task(colId:{},compId:{})'s nextTriggerTime = 0, this task will be disable]", task.getColId(), task.getCompId());
        }
        if (task.getEndTime() != null && nextValidTimeAfter > task.getEndTime()) {
            log.warn("[opt:calNextTriggerTime,message:current task(colId:{},compId:{})'s nextTriggerTime > endTime, this task will be disable]", task.getColId(), task.getCompId());
        }
        return nextValidTimeAfter;
    }

}
