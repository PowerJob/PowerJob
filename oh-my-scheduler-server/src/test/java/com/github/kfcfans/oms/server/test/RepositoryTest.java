package com.github.kfcfans.oms.server.test;

import com.github.kfcfans.common.utils.NetUtils;
import com.github.kfcfans.oms.server.common.constans.JobStatus;
import com.github.kfcfans.oms.server.common.constans.TimeExpressionType;
import com.github.kfcfans.oms.server.persistence.model.ExecuteLogDO;
import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.model.OmsLockDO;
import com.github.kfcfans.oms.server.persistence.repository.ExecuteLogRepository;
import com.github.kfcfans.oms.server.persistence.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.persistence.repository.OmsLockRepository;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * 数据库层测试
 *
 * @author tjq
 * @since 2020/4/5
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RepositoryTest {

    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private OmsLockRepository omsLockRepository;
    @Resource
    private ExecuteLogRepository executeLogRepository;

    /**
     * 需要证明批量写入失败后会回滚
     */
    @Test
    public void testBatchLock() {

        List<OmsLockDO> locks = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            OmsLockDO lockDO = new OmsLockDO("lock" + i, NetUtils.getLocalHost());
            locks.add(lockDO);
        }
        omsLockRepository.saveAll(locks);
        omsLockRepository.flush();
    }

    @Test
    public void testSelectCronJobSQL() {
        List<JobInfoDO> result = jobInfoRepository.findByAppIdInAndStatusAndTimeExpressionTypeAndNextTriggerTimeLessThanEqual(Lists.newArrayList(1L), JobStatus.ENABLE.getV(), TimeExpressionType.CRON.getV(), System.currentTimeMillis());
        System.out.println(result);
    }

    @Test
    public void testUpdate() {
        ExecuteLogDO updateEntity = new ExecuteLogDO();
        updateEntity.setId(22L);
        updateEntity.setActualTriggerTime(System.currentTimeMillis());
        updateEntity.setResult("hahaha");
        executeLogRepository.saveAndFlush(updateEntity);
    }

}
