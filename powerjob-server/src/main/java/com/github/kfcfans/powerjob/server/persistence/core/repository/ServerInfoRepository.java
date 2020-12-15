package com.github.kfcfans.powerjob.server.persistence.core.repository;

import com.github.kfcfans.powerjob.server.persistence.core.model.ServerInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import javax.persistence.LockModeType;
import java.util.List;

/**
 * 服务器信息 数据操作层
 *
 * @author tjq
 * @since 2020/4/15
 */
public interface ServerInfoRepository extends JpaRepository<ServerInfoDO, Long> {
    ServerInfoDO findByIp(String ip);

    @Query("select t from ServerInfoDO as t order by t.id asc")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ServerInfoDO> findAllAndLockTable();
}
