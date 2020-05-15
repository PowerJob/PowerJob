package com.github.kfcfans.oms.samples.service.lock;

import com.github.kfcfans.oms.common.utils.CommonUtils;
import com.github.kfcfans.oms.common.utils.NetUtils;
import com.github.kfcfans.oms.samples.persistence.core.model.OmsLockDO;
import com.github.kfcfans.oms.samples.persistence.core.repository.OmsLockRepository;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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

    private Map<String, AtomicInteger> lockName2FailedTimes = Maps.newConcurrentMap();
    private static final int MAX_FAILED_NUM = 5;
    // 最长持有锁30秒
    private static final long LOCK_TIMEOUT_MS = 30000;

    @Override
    public boolean lock(String name) {

        AtomicInteger failedCount = lockName2FailedTimes.computeIfAbsent(name, ignore -> new AtomicInteger(0));
        OmsLockDO newLock = new OmsLockDO(name, NetUtils.getLocalHost());
        try {
            omsLockRepository.saveAndFlush(newLock);
            failedCount.set(0);
            return true;
        }catch (DataIntegrityViolationException ignore) {
        }catch (Exception e) {
            log.warn("[DatabaseLockService] write lock to database failed, lockName = {}.", name, e);
        }

        // 连续失败一段时间，需要判断是否为锁释放失败的情况
        if (failedCount.incrementAndGet() > MAX_FAILED_NUM) {

            OmsLockDO omsLockDO = omsLockRepository.findByLockName(name);
            long lockedMillions = System.currentTimeMillis() - omsLockDO.getGmtCreate().getTime();
            if (lockedMillions > LOCK_TIMEOUT_MS) {

                log.warn("[DatabaseLockService] The lock({}) already timeout, will be deleted now.", omsLockDO);
                unlock(name);
            } else {
                failedCount.set(0);
            }
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

    @Override
    public boolean batchLock(List<String> names) {
        List<OmsLockDO> locks = Lists.newLinkedList();
        names.forEach(name -> locks.add(new OmsLockDO(name, NetUtils.getLocalHost())));
        try {
            omsLockRepository.saveAll(locks);
            omsLockRepository.flush();
            return true;
        }catch (DataIntegrityViolationException ignore) {
        }catch (Exception e) {
            log.warn("[DatabaseLockService] write locks to database failed, lockNames = {}.", names, e);
        }
        return false;
    }

    @Override
    public void batchUnLock(List<String> names) {
        try {
            CommonUtils.executeWithRetry0(() -> omsLockRepository.deleteByLockNames(names));
        }catch (Exception e) {
            log.error("[DatabaseLockService] unlocks {} failed.", names, e);
        }
    }
}
