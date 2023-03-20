package tech.powerjob.server.persistence.remote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.powerjob.server.persistence.remote.model.UserRoleDO;

/**
 * UserRoleRepository
 *
 * @author tjq
 * @since 2023/3/20
 */
public interface UserRoleRepository extends JpaRepository<UserRoleDO, Long> {
}
