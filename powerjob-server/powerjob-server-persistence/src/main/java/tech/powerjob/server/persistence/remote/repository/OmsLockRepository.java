package tech.powerjob.server.persistence.remote.repository;

import tech.powerjob.server.persistence.remote.model.OmsLockDO;
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
    @Transactional(rollbackOn = Exception.class)
    @Query(value = "delete from OmsLockDO where lockName = ?1")
    int deleteByLockName(String lockName);

    OmsLockDO findByLockName(String lockName);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    int deleteByOwnerIP(String ip);
}
