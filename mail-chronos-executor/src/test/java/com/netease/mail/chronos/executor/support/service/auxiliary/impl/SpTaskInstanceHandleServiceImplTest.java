package com.netease.mail.chronos.executor.support.service.auxiliary.impl;

import com.netease.mail.chronos.base.ServiceBaseTester;
import com.netease.mail.chronos.executor.support.base.po.TaskInstancePrimaryKey;
import com.netease.mail.chronos.executor.support.entity.SpRtTaskInstance;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Echo009
 * @since 2021/10/28
 */
public class SpTaskInstanceHandleServiceImplTest extends ServiceBaseTester {

    @Autowired
    private SpTaskInstanceHandleServiceImpl spTaskInstanceHandleService;


    @Transactional
    @Test
    public void testLoad(){
        insert();
        List<TaskInstancePrimaryKey> taskInstancePrimaryKeys = spTaskInstanceHandleService.loadHandleInstanceIdList();
        Assert.assertNotEquals(0, taskInstancePrimaryKeys.size());
    }






    public void insert(){
        SpRtTaskInstance origin = new SpRtTaskInstance();
        origin.setId(1L);
        origin.setPartitionKey(20211027);
        origin.setCustomId(1L);
        origin.setTaskId(1L);
        origin.setStatus(0);
        origin.setEnable(true);
        origin.setExpectedTriggerTime(0L);
        origin.setMaxRetryTimes(1);
        origin.setCreateTime(new Date());
        origin.setUpdateTime(new Date());
        spTaskInstanceHandleService.insert(origin);
    }


}