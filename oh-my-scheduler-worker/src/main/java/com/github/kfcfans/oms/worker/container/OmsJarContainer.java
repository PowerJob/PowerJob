package com.github.kfcfans.oms.worker.container;

import com.github.kfcfans.oms.common.ContainerConstant;
import com.github.kfcfans.oms.worker.common.OmsWorkerException;
import com.github.kfcfans.oms.worker.core.classloader.OhMyClassLoader;
import com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

/**
 * OMS 容器实现
 *
 * @author tjq
 * @since 2020/5/15
 */
@Slf4j
public class OmsJarContainer implements OmsContainer {

    @Getter
    private final Long id;
    private final String name;
    private final String localJarPath;

    private OhMyClassLoader containerClassLoader;
    private ClassPathXmlApplicationContext container;

    private Map<String, BasicProcessor> processorCache = Maps.newConcurrentMap();

    public OmsJarContainer(Long containerId, String containerName, String localJarPath) {
        this.id = containerId;
        this.name = containerName;
        this.localJarPath = localJarPath;
    }

    @Override
    public BasicProcessor getProcessor(String className) {

        return processorCache.computeIfAbsent(className, ignore -> {
            Class<?> targetClass;
            try {
                targetClass = containerClassLoader.loadClass(className);
            }catch (ClassNotFoundException cnf) {
                log.error("[OmsJarContainer-{}] can't find class: {} in container.", name, className);
                return null;
            }

            // 先尝试从 Spring IOC 容器加载
            try {
                return (BasicProcessor) container.getBean(targetClass);
            }catch (BeansException be) {
                log.warn("[OmsJarContainer-{}] load instance from spring container failed, try to build instance directly.", name);
            }catch (ClassCastException cce) {
                log.error("[OmsJarContainer-{}] {} should implements the Processor interface!", name, className);
                return null;
            }

            // 直接实例化
            try {
                Object obj = targetClass.getDeclaredConstructor().newInstance();
                BasicProcessor processor = (BasicProcessor) obj;
                processor.init();
                return processor;
            }catch (Exception e) {
                log.error("[OmsJarContainer-{}] load {} failed", name, className, e);
            }
            return null;
        });
    }

    @Override
    public void init() throws Exception {

        log.info("[OmsJarContainer] start to init container(id={},name={},jarPath={})", id, name, localJarPath);

        File file = new File(localJarPath);
        URL jarURL = file.toURI().toURL();

        // 创建类加载器
        this.containerClassLoader = new OhMyClassLoader(new URL[]{jarURL}, this.getClass().getClassLoader());

        // 获取资源文件
        URL propertiesURL = containerClassLoader.getResource(ContainerConstant.CONTAINER_PROPERTIES_FILE_NAME);
        URL springXmlURL = containerClassLoader.getResource(ContainerConstant.SPRING_CONTEXT_FILE_NAME);

        if (propertiesURL == null) {
            log.error("[OmsJarContainer] can't find {} in jar {}.", ContainerConstant.CONTAINER_PROPERTIES_FILE_NAME, localJarPath);
            throw new OmsWorkerException("invalid jar file");
        }
        if (springXmlURL == null) {
            log.error("[OmsJarContainer] can't find {} in jar {}.", ContainerConstant.SPRING_CONTEXT_FILE_NAME, localJarPath);
            throw new OmsWorkerException("invalid jar file");
        }

        // 解析 Properties
        Properties properties = new Properties();
        try (InputStream is = propertiesURL.openStream()) {
            properties.load(is);
            log.info("[OmsJarContainer] load container properties successfully: {}", properties);
        }
        String packageName = properties.getProperty(ContainerConstant.CONTAINER_PACKAGE_NAME_KEY);
        if (StringUtils.isEmpty(packageName)) {
            log.error("[OmsJarContainer] get package name failed, developer should't modify the properties file!");
            throw new OmsWorkerException("invalid jar file");
        }

        // 加载用户类
        containerClassLoader.load(packageName);

        // 创建 Spring IOC 容器
        this.container = new ClassPathXmlApplicationContext(new String[]{springXmlURL.getPath()}, false);
        this.container.setClassLoader(containerClassLoader);
        this.container.refresh();

        log.info("[OmsJarContainer] init container(id={},name={},jarPath={}) successfully", id, name, localJarPath);
    }

    @Override
    public void destroy() throws Exception {
        try {
            processorCache.clear();
            container.close();
            containerClassLoader.close();
            log.info("[OmsJarContainer-{}] container destroyed successfully", name);
        }catch (Exception e) {
            log.error("[OmsJarContainer-{}] container destroyed failed", name, e);
        }
    }
}
