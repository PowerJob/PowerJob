package tech.powerjob.server.infrastructure.config.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tech.powerjob.common.enums.ErrorCodes;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.server.extension.LockService;
import tech.powerjob.server.infrastructure.config.Config;
import tech.powerjob.server.infrastructure.config.DynamicServerConfigCrudService;
import tech.powerjob.server.persistence.remote.model.SundryDO;
import tech.powerjob.server.persistence.remote.repository.SundryRepository;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * DynamicServerConfigCrudService
 *
 * @author tjq
 * @since 2024/8/24
 */
@Slf4j
@Service
public class DynamicServerConfigCrudServiceImpl implements DynamicServerConfigCrudService {

    @Resource
    private LockService lockService;
    @Resource
    private SundryRepository sundryRepository;

    private static final String PKEY = "sys.powerjob.server.config";

    private static final int MAX_LOCK_TIME = 60000;

    @Override
    public void save(Config config) {
        String lockKey = PKEY.concat(config.getKey());
        boolean acquiredLock = lockService.tryLock(lockKey, MAX_LOCK_TIME);
        if (!acquiredLock) {
            throw new PowerJobException(ErrorCodes.SYS_ACQUIRE_LOCK_FAILED, "请稍后重试");
        }

        SundryDO sundryDO;

        try {
            Optional<SundryDO> oldConfigOpt = sundryRepository.findByPkeyAndSkey(PKEY, config.getKey());
            if (oldConfigOpt.isPresent()) {
                sundryDO = oldConfigOpt.get();
                fillConfig2SundryDO(sundryDO, config);
            } else {
                //  纯新增
                sundryDO = new SundryDO();
                fillConfig2SundryDO(sundryDO, config);
                sundryDO.setGmtCreate(new Date());
            }

            sundryRepository.saveAndFlush(sundryDO);
            log.info("[DynamicServerConfigCrudService] save config successfully, config: {}, sundry: {}", config, sundryDO);

        } finally {
            lockService.unlock(lockKey);
        }
    }

    @Override
    public Optional<Config> fetch(String key) {
        Optional<SundryDO> oldConfigOpt = sundryRepository.findByPkeyAndSkey(PKEY, key);
        return oldConfigOpt.map(DynamicServerConfigCrudServiceImpl::convert2Config);
    }

    @Override
    public void delete(String key) {
        Optional<Long> deletedOpt = sundryRepository.deleteByPkeyAndSkey(PKEY, key);
        if (deletedOpt.isPresent()) {
            log.info("[DynamicServerConfigCrudService] delete config[{}] successfully, origin data: {}", key, deletedOpt.get());
        } else {
            log.warn("[DynamicServerConfigCrudService] config[{}] not exist, no need to delete!", key);
        }
    }

    @Override
    public List<Config> list() {
        List<SundryDO> allByPkey = sundryRepository.findAllByPkey(PKEY);
        return Optional.ofNullable(allByPkey).orElse(Collections.emptyList()).stream().map(DynamicServerConfigCrudServiceImpl::convert2Config).collect(Collectors.toList());
    }

    private static void fillConfig2SundryDO(SundryDO sundryDO, Config config) {
        sundryDO.setPkey(PKEY);
        sundryDO.setSkey(config.getKey());
        sundryDO.setContent(config.getValue());
        sundryDO.setExtra(config.getComment());
        sundryDO.setGmtModified(new Date());
    }

    private static Config convert2Config(SundryDO sundryDO) {
        return new Config()
                .setKey(sundryDO.getSkey())
                .setValue(sundryDO.getContent())
                .setComment(sundryDO.getExtra());
    }
}
