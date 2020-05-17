package com.github.kfcfans.oms.server.persistence.core.repository;

import com.github.kfcfans.oms.server.persistence.core.model.ContainerInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 容器信息 数据操作层
 *
 * @author tjq
 * @since 2020/5/15
 */
public interface ContainerInfoRepository extends JpaRepository<ContainerInfoDO, Long> {

    List<ContainerInfoDO> findByAppId(Long appId);

    Optional<ContainerInfoDO> findByContainerName(String containerName);

}
