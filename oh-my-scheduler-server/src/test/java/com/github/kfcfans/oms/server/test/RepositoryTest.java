package com.github.kfcfans.oms.server.test;

import com.github.kfcfans.common.utils.NetUtils;
import com.github.kfcfans.oms.server.persistence.model.OmsLockDO;
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
    private OmsLockRepository omsLockRepository;

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

}
