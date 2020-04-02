package com.github.kfcfans.oms.server.persistence.repository;

import com.github.kfcfans.oms.server.persistence.model.AppInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * AppInfo 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface AppInfoRepository extends JpaRepository<AppInfoDO, Long> {
}
