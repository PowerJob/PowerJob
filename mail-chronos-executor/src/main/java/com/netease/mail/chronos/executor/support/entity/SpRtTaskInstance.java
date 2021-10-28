package com.netease.mail.chronos.executor.support.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.netease.mail.chronos.executor.support.entity.base.TaskInstance;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * 提醒任务实例,rt means remind task
 * @author  sp_rt_task_instance
 */
@TableName(value ="sp_rt_task_instance")
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class SpRtTaskInstance extends TaskInstance {



}