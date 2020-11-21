package com.github.kfcfans.powerjob.worker.common;

import com.github.kfcfans.powerjob.common.RemoteConstant;
import com.github.kfcfans.powerjob.worker.common.constants.StoreStrategy;
import com.github.kfcfans.powerjob.worker.core.processor.ProcessResult;
import com.google.common.collect.Lists;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Worker 配置文件
 *
 * @author tjq
 * @since 2020/3/16
 */
@Getter
@Setter
public class OhMyConfig {
    /**
     * 应用名称
     */
    private String appName;
    /**
     * 启动端口
     */
    private int port = RemoteConstant.DEFAULT_WORKER_PORT;
    /**
     * 调度服务器地址，ip:port 或 域名
     */
    private List<String> serverAddress = Lists.newArrayList();
    /**
     * 本地持久化方式，默认使用磁盘
     */
    private StoreStrategy storeStrategy = StoreStrategy.DISK;
    /**
     * 最大返回值长度，超过会被截断
     * {@link ProcessResult}#msg 的最大长度
     */
    private int maxResultLength = 8096;
    /**
     * 用户自定义上下文对象，该值会被透传到 TaskContext#userContext 属性
     * 使用场景：容器脚本Java处理器需要使用oms-worker宿主应用的Spring Bean，可在此处传入 ApplicationContext，在Processor中获取 bean
     */
    private Object userContext;
    /**
     * 启动测试模式，true情况下，不再尝试连接 server 并验证appName
     * true -> 用于本地写单元测试调试； false -> 默认值，标准模式
     */
    private boolean enableTestMode = false;
}
