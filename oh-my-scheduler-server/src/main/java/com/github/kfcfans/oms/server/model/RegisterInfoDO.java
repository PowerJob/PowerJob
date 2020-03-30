package com.github.kfcfans.oms.server.model;

/**
 * 注册信息表，用于服务发现
 *
 * @author tjq
 * @since 2020/3/30
 */
public class RegisterInfoDO extends BaseDO {

    private String appName;
    private String currentServerAddress;
    private String lastServerAddress;
}
