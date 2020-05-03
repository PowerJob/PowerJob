package com.github.kfcfans.oms.server.persistence.mongodb;

import lombok.Data;

/**
 * 任务日志元数据
 *
 * @author tjq
 * @since 2020/5/3
 */
@Data
public class InstanceLogMetadata {

    /**
     * 任务实例ID
     */
    private long instanceId;
    /**
     * 文件大小
     */
    private long fileSize;
    /**
     * 创建时间（用于条件删除）
     */
    private long createdTime;

}
