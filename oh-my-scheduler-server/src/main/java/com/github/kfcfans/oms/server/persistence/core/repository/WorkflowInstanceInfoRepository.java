package com.github.kfcfans.oms.server.persistence.core.repository;

import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInstanceInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 工作流运行实例数据操作
 *
 * @author tjq
 * @since 2020/5/26
 */
public interface WorkflowInstanceInfoRepository extends JpaRepository<WorkflowInstanceInfoDO, Long> {
}
