package tech.powerjob.server.persistence.remote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.powerjob.server.persistence.remote.model.SundryDO;

import java.util.List;
import java.util.Optional;

/**
 * SundryRepository
 *
 * @author tjq
 * @since 2024/2/15
 */
public interface SundryRepository extends JpaRepository<SundryDO, Long> {

    List<SundryDO> findAllByPkey(String pkey);

    Optional<SundryDO> findByPkeyAndSkey(String pkey, String skey);
}
