package com.github.kfcfans.powerjob.server.service.lock;

import com.github.kfcfans.powerjob.common.utils.CommonUtils;
import com.github.kfcfans.powerjob.common.utils.NetUtils;
import com.github.kfcfans.powerjob.server.persistence.core.model.OmsLockDO;
import com.github.kfcfans.powerjob.server.persistence.core.repository.OmsLockRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 基于数据库实现的分布式锁
 *
 * @author tjq
 * @since 2020/4/5
 */
@Slf4j
@Service
public class DatabaseLockService implements LockService {

    @Resource
    private OmsLockRepository omsLockRepository;

    @Override
    public boolean lock(String name, long maxLockTime) {

        OmsLockDO newLock = new OmsLockDO(name, NetUtils.getLocalHost(), maxLockTime);
        try {
            omsLockRepository.saveAndFlush(newLock);
            return true;
        } catch (DataIntegrityViolationException ignore) {
        } catch (Exception e) {
            log.warn("[DatabaseLockService] write lock to database failed, lockName = {}.", name, e);
        }

        OmsLockDO omsLockDO = omsLockRepository.findByLockName(name);
        long lockedMillions = System.currentTimeMillis() - omsLockDO.getGmtCreate().getTime();

        // 锁超时，强制释放锁并重新尝试获取
        if (lockedMillions > omsLockDO.getMaxLockTime()) {

            log.warn("[DatabaseLockService] The lock[{}] already timeout, will be unlocked now.", omsLockDO);
            unlock(name);
            return lock(name, maxLockTime);
        }
        return false;
    }

    @Override
    public void unlock(String name) {

        try {
            CommonUtils.executeWithRetry0(() -> omsLockRepository.deleteByLockName(name));
        }catch (Exception e) {
            log.error("[DatabaseLockService] unlock {} failed.", name, e);
        }
    }

}
