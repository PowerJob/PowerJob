package com.netease.mail.chronos.executor.support.base.po;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Echo009
 * @since 2021/10/28
 */
@Data
@Accessors(chain = true)
public class TaskInstancePrimaryKey {

    /**
     * id
     */
    private Long id;
    /**
     * 分区键
     */
    private Integer partitionKey;


}
