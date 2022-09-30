package tech.powerjob.server.persistence.remote.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.powerjob.server.persistence.remote.model.AppInfoDO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * AppInfo 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface AppInfoRepository extends JpaRepository<AppInfoDO, Long> {

    Optional<AppInfoDO> findByAppName(String appName);

    Page<AppInfoDO> findByAppNameLike(String condition, Pageable pageable);

    /**
     * 根据 currentServer 查询 appId
     * 其实只需要 id，处于性能考虑可以直接写SQL只返回ID
     */
    List<AppInfoDO> findAllByCurrentServer(String currentServer);

    @Query(value = "select id from AppInfoDO where currentServer = :currentServer")
    List<Long> listAppIdByCurrentServer(@Param("currentServer")String currentServer);

}
