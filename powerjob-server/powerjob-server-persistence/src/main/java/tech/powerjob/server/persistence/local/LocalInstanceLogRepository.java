package tech.powerjob.server.persistence.local;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import javax.transaction.Transactional;
import java.util.List;
import java.util.stream.Stream;

/**
 * 本地运行时日志数据操作层
 *
 * @author tjq
 * @since 2020/4/27
 */
public interface LocalInstanceLogRepository extends JpaRepository<LocalInstanceLogDO, Long> {

    /**
     * 流式查询
     */
    Stream<LocalInstanceLogDO> findByInstanceIdOrderByLogTime(Long instanceId);

    /**
     * 删除数据
     */
    @Modifying
    @Transactional(rollbackOn = Exception.class)
    long deleteByInstanceId(Long instanceId);

    @Modifying
    @Transactional(rollbackOn = Exception.class)
    @CanIgnoreReturnValue
    long deleteByInstanceIdInAndLogTimeLessThan(List<Long> instanceIds, Long t);

    long countByInstanceId(Long instanceId);
}
