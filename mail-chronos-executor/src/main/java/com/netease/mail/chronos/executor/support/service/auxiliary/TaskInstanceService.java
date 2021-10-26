package com.netease.mail.chronos.executor.support.service.auxiliary;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.netease.mail.chronos.executor.support.entity.base.TaskInstance;
import com.netease.mail.chronos.executor.support.enums.TaskInstanceHandleStrategy;

import java.util.List;

/**
 * @author Echo009
 * @since 2021/10/26
 */
public interface TaskInstanceService<T extends TaskInstance>  {


    /**
     * 返回匹配的策略信息
     * @return TaskInstanceHandleStrategy
     */
    TaskInstanceHandleStrategy matchStrategy();

    /**
     * 加载需要处理的任务实例 id 列表
     * @return id 列表
     */
    List<Long> loadHandleInstanceIdList(){

        BaseMapper<T> mapper = getMapper();
        LambdaQueryChainWrapper lambdaQueryChainWrapper = new LambdaQueryChainWrapper(mapper);



    }


    BaseMapper<T> getMapper();

}
