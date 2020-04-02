package com.github.kfcfans.oms.server.test;

import com.github.kfcfans.oms.server.service.LockService;
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

}
