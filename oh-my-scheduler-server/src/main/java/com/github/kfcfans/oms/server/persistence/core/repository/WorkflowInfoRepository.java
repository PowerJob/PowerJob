package com.github.kfcfans.oms.server.persistence.core.repository;

import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * DAG 工作流 数据操作层
 *
 * @author tjq
 * @since 2020/5/26
 */
public interface WorkflowInfoRepository extends JpaRepository<WorkflowInfoDO, Long> {


    List<WorkflowInfoDO> findByAppIdInAndStatusAndTimeExpressionTypeAndNextTriggerTimeLessThanEqual(List<Long> appIds, int status, int timeExpressionType, long time);

}
