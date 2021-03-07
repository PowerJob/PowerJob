package tech.powerjob.server.test;

import tech.powerjob.server.core.uid.IdGenerateService;
import tech.powerjob.server.extension.LockService;
import tech.powerjob.server.service.timing.CleanService;
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

        lockService.tryLock(lockName, 10000);
        lockService.tryLock(lockName, 10000);
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
