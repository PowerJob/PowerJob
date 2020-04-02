package com.github.kfcfans.oms.server.service.impl;

import com.github.kfcfans.common.utils.CommonUtils;
import com.github.kfcfans.common.utils.NetUtils;
import com.github.kfcfans.oms.server.persistence.model.OmsLockDO;
import com.github.kfcfans.oms.server.persistence.repository.OmsLockRepository;
import com.github.kfcfans.oms.server.service.LockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 基于数据库实现分布式锁
 *
 * @author tjq
 * @since 2020/4/2
 */
@Slf4j
@Service
public class DatabaseLockService implements LockService {

    @Resource
    private OmsLockRepository omsLockRepository;

    @Override
    public boolean lock(String name) {

        try {

            OmsLockDO lock = new OmsLockDO(name, NetUtils.getLocalHost());
            omsLockRepository.saveAndFlush(lock);

            return true;
        }catch (DataIntegrityViolationException ignore) {
            log.info("[DatabaseLockService] other thread get the lock {}.", name);
        } catch (Exception e) {
            log.error("[DatabaseLockService] lock {} failed.", name, e);
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
