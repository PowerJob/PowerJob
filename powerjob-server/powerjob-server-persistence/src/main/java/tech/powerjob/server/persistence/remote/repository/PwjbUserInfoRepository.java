package tech.powerjob.server.persistence.remote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.powerjob.server.persistence.remote.model.PwjbUserInfoDO;

import java.util.Optional;

/**
 * PwjbUserInfoRepository
 *
 * @author tjq
 * @since 2024/2/13
 */
public interface PwjbUserInfoRepository extends JpaRepository<PwjbUserInfoDO, Long> {

    Optional<PwjbUserInfoDO> findByUsername(String username);
}
