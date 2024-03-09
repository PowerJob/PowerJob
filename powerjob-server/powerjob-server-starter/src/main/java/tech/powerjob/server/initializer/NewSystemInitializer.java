package tech.powerjob.server.initializer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.extension.LockService;
import tech.powerjob.server.persistence.remote.model.SundryDO;
import tech.powerjob.server.persistence.remote.repository.SundryRepository;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * 新系统初始化器
 *
 * @author tjq
 * @since 2023/9/5
 */
@Slf4j
@Component
public class NewSystemInitializer implements CommandLineRunner {


    private static final String LOCK_PREFIX = "sys_init_lock_";

    private static final int MAX_LOCK_TIME = 5000;


    @Resource
    private LockService lockService;
    @Resource
    private SundryRepository sundryRepository;
    @Resource
    private SystemInitializeService systemInitializeService;

    private static final String SUNDRY_PKEY = "sys_initialize";

    @Override
    public void run(String... args) throws Exception {
        initSystemAdmin();
        initDefaultNamespace();
    }

    private void initSystemAdmin() {
        clusterInit(SystemInitializeService.GOAL_INIT_ADMIN, Void -> systemInitializeService.initAdmin());
    }

    private void initDefaultNamespace() {
        clusterInit(SystemInitializeService.GOAL_INIT_NAMESPACE, Void -> systemInitializeService.initNamespace());
    }

    private void clusterInit(String name, Consumer<Void> initFunc) {

        Optional<SundryDO> sundryOpt = sundryRepository.findByPkeyAndSkey(SUNDRY_PKEY, name);
        if (sundryOpt.isPresent()) {
            log.info("[NewSystemInitializer] already initialized, skip: {}", name);
            return;
        }

        String lockName = LOCK_PREFIX.concat(name);

        while (true) {
            try {

                boolean lockStatus = lockService.tryLock(lockName, MAX_LOCK_TIME);

                // 无论是否拿到锁，都重现检测一次，如果已完成初始化，则直接 return
                Optional<SundryDO> sundryOpt2 = sundryRepository.findByPkeyAndSkey(SUNDRY_PKEY, name);
                if (sundryOpt2.isPresent()) {
                    log.info("[NewSystemInitializer] other server finished initialize, skip process: {}", name);
                    break;
                }

                if (!lockStatus) {
                    CommonUtils.easySleep(277);
                    continue;
                }

                log.info("[NewSystemInitializer] try to initialize: {}", name);
                initFunc.accept(null);
                log.info("[NewSystemInitializer] initialize [{}] successfully!", name);

                // 写入初始化成功标记
                SundryDO sundryDO = new SundryDO();
                sundryDO.setPkey(SUNDRY_PKEY);
                sundryDO.setSkey(name);
                sundryDO.setContent("A");
                sundryDO.setGmtCreate(new Date());
                sundryRepository.saveAndFlush(sundryDO);
                log.info("[NewSystemInitializer] write initialized tag successfully: {}", sundryDO);

                break;
            } finally {
                lockService.unlock(lockName);
            }
        }

    }
}
