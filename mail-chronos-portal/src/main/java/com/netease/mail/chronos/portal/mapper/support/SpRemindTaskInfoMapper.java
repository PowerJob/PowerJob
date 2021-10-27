package com.netease.mail.chronos.portal.mapper.support;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.netease.mail.chronos.portal.entity.support.SpRemindTaskInfo;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author  com.netease.mail.chronos.portal.entity.support.SpRemindTaskInfo
 */
public interface SpRemindTaskInfoMapper extends BaseMapper<SpRemindTaskInfo> {

    /**
     * 根据 comp_id 删除任务
     * @param compId 组件 id
     * @return 被删除的任务个数
     */
    int deleteTaskByCompId(@Param("compId") String compId);


    /**
     * 查找 ID 列表
     * @param compId 组件 ID
     * @return id 列表
     */
    List<Long> selectIdListByCompId(@Param("compId")String compId);
}




