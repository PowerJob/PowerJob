package tech.powerjob.server.persistence.remote.repository;

import tech.powerjob.server.persistence.remote.model.UserInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用户信息表数据库访问层
 *
 * @author tjq
 * @since 2020/4/12
 */
public interface UserInfoRepository extends JpaRepository<UserInfoDO, Long> {

    List<UserInfoDO> findByUsernameLike(String username);

    List<UserInfoDO> findByIdIn(List<Long> userIds);
}
