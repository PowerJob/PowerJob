package com.github.kfcfans.oms.server.test;

import com.github.kfcfans.oms.server.service.id.IdGenerateService;
import com.github.kfcfans.oms.server.service.lock.LockService;
import com.github.kfcfans.oms.server.service.timing.CleanService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 服务测试
 *
 * @author tjq
 * @since 2020/4/2
 */
//@ActiveProfiles("daily")
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ServiceTest {

    @Resource
    private LockService lockService;
    @Resource
    private IdGenerateService idGenerateService;
    @Resource
    private CleanService cleanService;

    @Test
    public void testLockService() {
        String lockName = "myLock";

        lockService.lock(lockName, 10000);
        lockService.lock(lockName, 10000);
        lockService.unlock(lockName);
    }

    @Test
    public void testIdGenerator() {
        System.out.println(idGenerateService.allocate());
    }

    @Test
    public void testCleanInstanceInfo() {
        cleanService.cleanInstanceLog();
    }

}
