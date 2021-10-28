package com.netease.mail.chronos.mapper;

import com.netease.mail.chronos.base.DaoBaseContext;
import com.netease.mail.chronos.executor.support.base.po.TaskInstancePrimaryKey;
import com.netease.mail.chronos.executor.support.entity.SpRtTaskInstance;
import com.netease.mail.chronos.executor.support.mapper.SpRtTaskInstanceMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author Echo009
 * @since 2021/10/26
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DaoBaseContext.class})
@ActiveProfiles(value = "local")
@Slf4j
public class MapperTest {

    @Autowired
    private SpRtTaskInstanceMapper spRtTaskInstanceMapper;

    @Test
    @Transactional
    public void testSpRtTaskInstanceMapper() {

        SpRtTaskInstance origin = new SpRtTaskInstance();
        origin.setId(1L);
        origin.setPartitionKey(20211027);
        origin.setCustomId(1L);
        origin.setTaskId(1L);
        origin.setStatus(0);
        origin.setEnable(true);
        origin.setExpectedTriggerTime(1L);
        origin.setMaxRetryTimes(1);
        origin.setCreateTime(new Date());
        origin.setUpdateTime(new Date());

        int insert = spRtTaskInstanceMapper.insert(origin);

        SpRtTaskInstance inserted = spRtTaskInstanceMapper.selectByPrimaryKey(origin.getId(), origin.getPartitionKey());

        log.info("inserted:{}",inserted);
        Assert.assertNotNull("insert failed!",inserted);

        // just test sql
        List<Integer> partitionList = new ArrayList<>();
        partitionList.add(20211028);
        partitionList.add(20211027);
        List<TaskInstancePrimaryKey> taskInstancePrimaryKeys = spRtTaskInstanceMapper.selectIdListOfNeedTriggerInstance(1L, partitionList);

        for (TaskInstancePrimaryKey taskInstancePrimaryKey : taskInstancePrimaryKeys) {
            log.info("needTriggerInstanceKey:{}",taskInstancePrimaryKey);
        }
        // update

        origin.setStatus(3);
        origin.setEnable(false);
        origin.setRunningTimes(1);
        origin.setUpdateTime(new Date());
        origin.setExpectedTriggerTime(-1L);

        spRtTaskInstanceMapper.updateByPrimaryKey(origin);

        SpRtTaskInstance afterUpdate  = spRtTaskInstanceMapper.selectByPrimaryKey(origin.getId(), origin.getPartitionKey());
        log.info("afterUpdate:{}",afterUpdate);
        Assert.assertEquals(afterUpdate.getExpectedTriggerTime(),inserted.getExpectedTriggerTime());


    }


}
