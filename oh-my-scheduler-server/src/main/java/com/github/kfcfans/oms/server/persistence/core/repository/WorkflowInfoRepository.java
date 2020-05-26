package com.github.kfcfans.oms.server.persistence.core.repository;

import com.github.kfcfans.oms.server.persistence.core.model.WorkflowInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * DAG 工作流 数据操作层
 *
 * @author tjq
 * @since 2020/5/26
 */
public interface WorkflowInfoRepository extends JpaRepository<WorkflowInfoDO, Long> {
}
