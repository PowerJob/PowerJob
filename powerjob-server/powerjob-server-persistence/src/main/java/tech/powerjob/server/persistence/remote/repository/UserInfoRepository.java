package tech.powerjob.server.persistence.remote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.powerjob.server.persistence.remote.model.UserInfoDO;

import java.util.List;
import java.util.Optional;

/**
 * 用户信息表数据库访问层
 *
 * @author tjq
 * @since 2020/4/12
 */
public interface UserInfoRepository extends JpaRepository<UserInfoDO, Long>, JpaSpecificationExecutor<UserInfoDO> {

    Optional<UserInfoDO> findByUsername(String username);

    List<UserInfoDO> findByUsernameLike(String username);

    List<UserInfoDO> findByIdIn(List<Long> userIds);
}
