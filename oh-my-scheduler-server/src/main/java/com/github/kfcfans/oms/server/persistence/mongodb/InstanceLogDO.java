package com.github.kfcfans.oms.server.persistence.mongodb;

import javax.persistence.Id;

/**
 * 任务实例的运行时日志
 *
 * @author tjq
 * @since 2020/4/27
 */
public class InstanceLogDO {

    @Id
    private String id;

    private String log;
}
