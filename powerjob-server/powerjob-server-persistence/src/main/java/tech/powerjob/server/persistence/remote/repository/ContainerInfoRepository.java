package tech.powerjob.server.persistence.remote.repository;

import tech.powerjob.server.persistence.remote.model.ContainerInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 容器信息 数据操作层
 *
 * @author tjq
 * @since 2020/5/15
 */
public interface ContainerInfoRepository extends JpaRepository<ContainerInfoDO, Long> {

    List<ContainerInfoDO> findByAppIdAndStatusNot(Long appId, Integer status);
}
