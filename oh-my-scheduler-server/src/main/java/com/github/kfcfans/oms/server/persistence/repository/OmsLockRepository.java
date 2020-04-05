package com.github.kfcfans.oms.server.persistence.repository;

import com.github.kfcfans.oms.server.persistence.model.OmsLockDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;

/**
 * 利用唯一性约束作为数据库锁
 *
 * @author tjq
 * @since 2020/4/2
 */
public interface OmsLockRepository extends JpaRepository<OmsLockDO, Long> {

    @Modifying
    @Transactional
    @Query(value = "delete from oms_lock where lock_name = ?1", nativeQuery = true)
    int deleteByLockName(String lockName);

    OmsLockDO findByLockName(String lockName);
}
