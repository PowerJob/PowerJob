package tech.powerjob.server.initializer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.common.constants.SwitchableStatus;
import tech.powerjob.server.extension.LockService;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import tech.powerjob.server.persistence.remote.repository.NamespaceRepository;
import tech.powerjob.server.persistence.remote.repository.UserInfoRepository;

import javax.annotation.Resource;
import java.util.Date;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 新系统初始化器
 *
 * @author tjq
 * @since 2023/9/5
 */
@Slf4j
@Component
public class NewSystemInitializer implements CommandLineRunner {

    private static final String SYSTEM_ADMIN_NAME = "ADMIN";
    private static final String SYSTEM_ADMIN_PASSWORD = "POWERJOB123456789";

    private static final String SYSTEM_DEFAULT_NAMESPACE = "default_namespace";

    private static final String LOCK_PREFIX = "sys_init_lock_";

    private static final int MAX_LOCK_TIME = 5000;

    @Resource
    private LockService lockService;
    @Resource
    private UserInfoRepository userInfoRepository;
    @Resource
    private NamespaceRepository namespaceRepository;

    @Override
    public void run(String... args) throws Exception {
        initSystemAdmin();
        initDefaultNamespace();
    }

    private void initSystemAdmin() {
        clusterInit("admin", () -> {
            Optional<UserInfoDO> systemAdminUserOpt = userInfoRepository.findByUsername(SYSTEM_ADMIN_NAME);
            return systemAdminUserOpt.isPresent();
        }, Void -> {

            UserInfoDO userInfoDO = new UserInfoDO();
            userInfoDO.setUsername(SYSTEM_ADMIN_NAME);
            userInfoDO.setNick(SYSTEM_ADMIN_NAME);
            userInfoDO.setPassword(SYSTEM_ADMIN_PASSWORD);
            userInfoDO.setGmtCreate(new Date());
            userInfoDO.setGmtModified(new Date());

            userInfoRepository.saveAndFlush(userInfoDO);

            log.info("[NewSystemInitializer] initSystemAdmin successfully!");

            // 循环10遍，强提醒用户，第一次使用必须更改 admin 密码
            for (int i = 0; i < 10; i++) {
                log.warn("The system has automatically created a super administrator account[username={},password={}], please log in and change the password immediately!", SYSTEM_ADMIN_NAME, SYSTEM_ADMIN_PASSWORD);
            }
        });
    }

    private void initDefaultNamespace() {
        clusterInit("namespace", () -> {
            Optional<NamespaceDO> namespaceOpt = namespaceRepository.findByCode(SYSTEM_DEFAULT_NAMESPACE);
            return namespaceOpt.isPresent();
        }, Void -> {
            NamespaceDO namespaceDO = new NamespaceDO();
            namespaceDO.setCode(SYSTEM_DEFAULT_NAMESPACE);
            namespaceDO.setName(SYSTEM_DEFAULT_NAMESPACE);
            namespaceDO.setStatus(SwitchableStatus.ENABLE.getV());

            namespaceRepository.saveAndFlush(namespaceDO);

            log.info("[NewSystemInitializer] initDefaultNamespace successfully!");
        });
    }

    private void clusterInit(String name, Supplier<Boolean> initialized, Consumer<Void> initFunc) {

        if (initialized.get()) {
            return;
        }

        String lockName = LOCK_PREFIX.concat(name);
        lockService.tryLock(lockName, MAX_LOCK_TIME);

        while (true) {
            boolean lockStatus = lockService.tryLock(lockName, MAX_LOCK_TIME);
            if (!lockStatus) {
                CommonUtils.easySleep(277);
                continue;
            }

            try {
                if (initialized.get()) {
                    return;
                }

                log.info("[NewSystemInitializer] try to initialize: {}", name);
                initFunc.accept(null);
                log.info("[NewSystemInitializer] initialize [{}] successfully!", name);
                return;
            } finally {
                lockService.unlock(lockName);
            }
        }

    }
}
