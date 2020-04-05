package com.github.kfcfans.oms.server.test;

import com.github.kfcfans.oms.server.service.lock.LockService;
import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * 服务测试
 *
 * @author tjq
 * @since 2020/4/2
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServiceTest {

    @Resource
    private LockService lockService;

    @Test
    public void testLockService() {
        String lockName = "myLock";

        lockService.lock(lockName);
        lockService.lock(lockName);
        lockService.unlock(lockName);
    }

    @Test
    public void testBatchLock() {
        List<String> lockNames = Lists.newArrayList("a", "b", "C", "d", "e");
        System.out.println(lockService.batchLock(lockNames));
        System.out.println(lockService.batchLock(lockNames));
    }

    @Test
    public void testBatchUnLock() {
        List<String> lockNames = Lists.newArrayList("a", "b", "C", "d", "e");
        lockService.batchUnLock(lockNames);
    }

}
