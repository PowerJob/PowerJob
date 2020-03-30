package com.github.kfcfans.oms.server.model;

import java.util.Date;

/**
 * 数据库实体类基类
 *
 * @author tjq
 * @since 2020/3/29
 */
public abstract class BaseDO {

    /**
     * 主键
     */
    private Long id;

    private Date gmtCreate;
    private Date gmtModified;

    private String attribute;
}
