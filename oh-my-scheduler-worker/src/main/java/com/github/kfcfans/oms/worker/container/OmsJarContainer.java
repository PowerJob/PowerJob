package com.github.kfcfans.oms.worker.container;

import com.github.kfcfans.oms.worker.core.classloader.OhMyClassLoader;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * OMS 容器实现
 *
 * @author tjq
 * @since 2020/5/15
 */
public class OmsJarContainer implements OmsContainer {

    private Long containerId;
    private String containerName;
    private String localJarPath;
    private OhMyClassLoader containerClassLoader;
    private ClassPathXmlApplicationContext container;

    public OmsJarContainer(Long containerId, String containerName, String localJarPath) {
        this.containerId = containerId;
        this.containerName = containerName;
        this.localJarPath = localJarPath;

        // 创建类加载器

    }

    @Override
    public Long getContainerId() {
        return containerId;
    }
    @Override
    public String getContainerName() {
        return containerName;
    }
    @Override
    public String getLocalJarPath() {
        return localJarPath;
    }
    @Override
    public OhMyClassLoader getContainerClassLoader() {
        return containerClassLoader;
    }
    @Override
    public ClassPathXmlApplicationContext getContainer() {
        return container;
    }
}
