package tech.powerjob.server.test;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tech.powerjob.common.enums.InstanceStatus;
import tech.powerjob.common.enums.TimeExpressionType;
import tech.powerjob.common.enums.WorkflowInstanceStatus;
import tech.powerjob.common.utils.NetUtils;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.persistence.remote.model.InstanceInfoDO;
import tech.powerjob.server.persistence.remote.model.JobInfoDO;
import tech.powerjob.server.persistence.remote.model.OmsLockDO;
import tech.powerjob.server.persistence.remote.model.brief.BriefInstanceInfo;
import tech.powerjob.server.persistence.remote.repository.InstanceInfoRepository;
import tech.powerjob.server.persistence.remote.repository.JobInfoRepository;
import tech.powerjob.server.persistence.remote.repository.OmsLockRepository;
import tech.powerjob.server.persistence.remote.repository.WorkflowInstanceInfoRepository;

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
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RepositoryTest {

    @Resource
    private JobInfoRepository jobInfoRepository;
    @Resource
    private OmsLockRepository omsLockRepository;
    @Resource
    private InstanceInfoRepository instanceInfoRepository;
    @Resource
    private WorkflowInstanceInfoRepository workflowInstanceInfoRepository;

    /**
     * 需要证明批量写入失败后会回滚
     */
    @Test
    @Transactional
    @Rollback
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
    public void testDeleteLock() {
        String lockName = "test-lock";
        OmsLockDO lockDO = new OmsLockDO(lockName, NetUtils.getLocalHost(), 10000L);
        omsLockRepository.save(lockDO);
        omsLockRepository.deleteByLockName(lockName);
    }

    @Test
    public void testSelectCronJobSQL() {
        List<JobInfoDO> result = jobInfoRepository.findByAppIdInAndStatusAndTimeExpressionTypeAndNextTriggerTimeLessThanEqual(Lists.newArrayList(1L), SwitchableStatus.ENABLE.getV(), TimeExpressionType.CRON.getV(), System.currentTimeMillis());
        System.out.println(result);
    }

    @Test
    @Transactional
    public void testUpdate() {
        InstanceInfoDO updateEntity = new InstanceInfoDO();
        updateEntity.setId(22L);
        updateEntity.setActualTriggerTime(System.currentTimeMillis());
        updateEntity.setResult("hahaha");
        instanceInfoRepository.saveAndFlush(updateEntity);
    }

    @Test
    @Transactional
    public void testExecuteLogUpdate() {
        instanceInfoRepository.update4TriggerFailed(1586310414570L, 2, System.currentTimeMillis(), System.currentTimeMillis(), "192.168.1.1", "NULL", new Date());
        instanceInfoRepository.update4FrequentJob(1586310419650L, 2, 200, new Date());
    }

    @Test
    public void testCheckQuery() {
        Date time = new Date();
        System.out.println(time);
        final List<BriefInstanceInfo> res = instanceInfoRepository.selectBriefInfoByAppIdInAndStatusAndGmtModifiedBefore(Lists.newArrayList(1L), 3, time, PageRequest.of(0, 100));
        System.out.println(res);
    }

    @Test
    public void testFindByJobIdInAndStatusIn() {
        List<Long> res = instanceInfoRepository.findByJobIdInAndStatusIn(Lists.newArrayList(1L, 2L, 3L, 4L), Lists.newArrayList(1, 2, 3, 4, 5));
        System.out.println(res);
    }

    @Test
    public void testDeleteInstanceInfo() {
        instanceInfoRepository.deleteAllByGmtModifiedBeforeAndStatusIn(new Date(), InstanceStatus.FINISHED_STATUS);
    }

    @Test
    public void testDeleteWorkflowInstanceInfo() {
        workflowInstanceInfoRepository.deleteAllByGmtModifiedBeforeAndStatusIn(new Date(), WorkflowInstanceStatus.FINISHED_STATUS);
    }

}
