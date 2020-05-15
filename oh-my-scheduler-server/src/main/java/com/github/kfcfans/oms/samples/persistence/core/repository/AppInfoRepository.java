package com.github.kfcfans.oms.samples.persistence.core.repository;

import com.github.kfcfans.oms.samples.persistence.core.model.AppInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * AppInfo 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface AppInfoRepository extends JpaRepository<AppInfoDO, Long> {

    AppInfoDO findByAppName(String appName);

    List<AppInfoDO> findByAppNameLike(String condition);

    /**
     * 根据 currentServer 查询 appId
     * 其实只需要 id，处于性能考虑可以直接写SQL只返回ID
     */
    List<AppInfoDO> findAllByCurrentServer(String currentServer);
}
