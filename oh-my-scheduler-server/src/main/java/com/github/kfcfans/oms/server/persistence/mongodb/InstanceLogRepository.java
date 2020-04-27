package com.github.kfcfans.oms.server.persistence.mongodb;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * 任务实例的运行时日志 MongoDB数据操作
 *
 * @author tjq
 * @since 2020/4/27
 */
public interface InstanceLogRepository extends MongoRepository<InstanceLogDO, String> {
}
