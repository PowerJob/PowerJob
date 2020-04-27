package com.github.kfcfans.oms.server.persistence.local;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 本地运行时日志数据操作层
 *
 * @author tjq
 * @since 2020/4/27
 */
public interface LocalInstanceLogRepository extends JpaRepository<LocalInstanceLogDO, Long> {
}
