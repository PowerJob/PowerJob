package tech.powerjob.server.persistence.remote.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tech.powerjob.server.persistence.remote.model.UserRoleDO;

import java.util.List;

/**
 * AppInfo 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface UserRoleRepository extends JpaRepository<UserRoleDO, Long> {
    /**
     * 查出指定用户分配的角色信息
     *
     * @param userId userId
     * @return .
     */
    List<UserRoleDO> findAllByUserId(Long userId);


}
