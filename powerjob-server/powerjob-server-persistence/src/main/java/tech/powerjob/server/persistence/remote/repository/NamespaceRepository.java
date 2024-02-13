package tech.powerjob.server.persistence.remote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.powerjob.server.persistence.remote.model.NamespaceDO;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 命名空间
 *
 * @author tjq
 * @since 2023/9/3
 */
public interface NamespaceRepository extends JpaRepository<NamespaceDO, Long>, JpaSpecificationExecutor<NamespaceDO> {

    Optional<NamespaceDO> findByCode(String code);

    List<NamespaceDO> findAllByIdIn(Collection<Long> ids);
}
