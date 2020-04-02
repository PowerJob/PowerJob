package com.github.kfcfans.oms.server.persistence.repository;

import com.github.kfcfans.oms.server.persistence.model.JobInfoDO;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JobInfo 数据访问层
 *
 * @author tjq
 * @since 2020/4/1
 */
public interface JobInfoRepository extends JpaRepository<JobInfoDO, Long> {
}
