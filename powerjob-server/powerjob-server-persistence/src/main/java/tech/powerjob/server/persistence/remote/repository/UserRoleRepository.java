package tech.powerjob.server.persistence.remote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.powerjob.server.persistence.remote.model.UserRoleDO;

import java.util.List;

/**
 * UserRoleRepository
 *
 * @author tjq
 * @since 2023/3/20
 */
public interface UserRoleRepository extends JpaRepository<UserRoleDO, Long> {

    List<UserRoleDO> findAllByUserId(Long userId);

    List<UserRoleDO> findAllByScopeAndTarget(Integer scope, Long target);

    List<UserRoleDO> findAllByScopeAndTargetAndRoleAndUserId(Integer scope, Long target, Integer role, Long userId);

    List<UserRoleDO> findAllByUserIdAndScope(Long userId, Integer scope);
}
