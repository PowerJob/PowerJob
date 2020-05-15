package com.github.kfcfans.oms.worker.container;

import com.github.kfcfans.oms.worker.core.classloader.OhMyClassLoader;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * OhMyScheduler 容器规范
 *
 * @author tjq
 * @since 2020/5/15
 */
public interface OmsContainer {

    Long getContainerId();
    String getContainerName();
    String getLocalJarPath();

    OhMyClassLoader getContainerClassLoader();
    ClassPathXmlApplicationContext getContainer();
}
