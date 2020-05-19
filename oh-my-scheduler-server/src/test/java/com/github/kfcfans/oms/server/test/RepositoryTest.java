package com.github.kfcfans.oms.server.test;

import com.github.kfcfans.oms.common.TimeExpressionType;
import com.github.kfcfans.oms.common.utils.NetUtils;
import com.github.kfcfans.oms.server.common.constans.JobStatus;
import com.github.kfcfans.oms.server.persistence.core.model.InstanceInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.JobInfoDO;
import com.github.kfcfans.oms.server.persistence.core.model.OmsLockDO;
import com.github.kfcfans.oms.server.persistence.core.repository.InstanceInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.JobInfoRepository;
import com.github.kfcfans.oms.server.persistence.core.repository.OmsLockRepository;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

/**
 * 数据库层测试
 *
 * @author tjq
 * @since 2020/4/5
 */
//@ActiveProfiles("daily")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RepositoryTest {

    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private OmsLockRepository omsLockRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;

    /**
     * 需要证明批量写入失败后会回滚
     */
    @Test
    public void testBatchLock() {

        List<OmsLockDO> locks = Lists.newArrayList();
        for (int i = 0; i < 10; i++) {
            OmsLockDO lockDO = new OmsLockDO("lock" + i, NetUtils.getLocalHost(), 10000L);
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
        InstanceInfoDO updateEntity = new InstanceInfoDO();
        updateEntity.setId(22L);
        updateEntity.setActualTriggerTime(System.currentTimeMillis());
        updateEntity.setResult("hahaha");
        instanceInfoRepository.saveAndFlush(updateEntity);
    }

    @Test
    public void testExecuteLogUpdate() {
        instanceInfoRepository.update4TriggerFailed(1586310414570L, 2, 100, System.currentTimeMillis(), System.currentTimeMillis(), "192.168.1.1", "NULL", "");
        instanceInfoRepository.update4FrequentJob(1586310419650L, 2, 200);
    }

    @Test
    public void testCheckQuery() {
        Date time = new Date();
        System.out.println(time);
        final List<InstanceInfoDO> res = instanceInfoRepository.findByAppIdInAndStatusAndGmtModifiedBefore(Lists.newArrayList(1L), 3, time);
        System.out.println(res);
    }

}
