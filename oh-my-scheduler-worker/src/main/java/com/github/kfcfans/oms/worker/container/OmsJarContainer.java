package com.github.kfcfans.oms.worker.container;

import com.github.kfcfans.oms.common.ContainerConstant;
import com.github.kfcfans.oms.worker.common.OmsWorkerException;
import com.github.kfcfans.oms.worker.core.processor.sdk.BasicProcessor;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OMS 容器实现
 *
 * @author tjq
 * @since 2020/5/15
 */
@Slf4j
public class OmsJarContainer implements OmsContainer {

    private final Long containerId;
    private final String name;
    private final String version;
    private final File localJarFile;
    private final Long deployedTime;

    // 引用计数器
    private final AtomicInteger referenceCount = new AtomicInteger(0);

    private OhMyClassLoader containerClassLoader;
    private ClassPathXmlApplicationContext container;

    private Map<String, BasicProcessor> processorCache = Maps.newConcurrentMap();

    public OmsJarContainer(Long containerId, String name, String version, File localJarFile) {
        this.containerId = containerId;
        this.name = name;
        this.version = version;
        this.localJarFile = localJarFile;
        this.deployedTime = System.currentTimeMillis();
    }

    @Override
    public BasicProcessor getProcessor(String className) {

        BasicProcessor basicProcessor = processorCache.computeIfAbsent(className, ignore -> {
            Class<?> targetClass;
            try {
                targetClass = containerClassLoader.loadClass(className);
            } catch (ClassNotFoundException cnf) {
                log.error("[OmsJarContainer-{}] can't find class: {} in container.", name, className);
                return null;
            }

            // 先尝试从 Spring IOC 容器加载
            try {
                return (BasicProcessor) container.getBean(targetClass);
            } catch (BeansException be) {
                log.warn("[OmsJarContainer-{}] load instance from spring container failed, try to build instance directly.", name);
            } catch (ClassCastException cce) {
                log.error("[OmsJarContainer-{}] {} should implements the Processor interface!", name, className);
                return null;
            } catch (Exception e) {
                log.error("[OmsJarContainer-{}] get bean failed for {}.", name, className, e);
                return null;
            }

            // 直接实例化
            try {
                Object obj = targetClass.getDeclaredConstructor().newInstance();
                BasicProcessor processor = (BasicProcessor) obj;
                processor.init();
                return processor;
            } catch (Exception e) {
                log.error("[OmsJarContainer-{}] load {} failed", name, className, e);
            }
            return null;
        });

        if (basicProcessor != null) {
            // 引用计数 + 1
            referenceCount.getAndIncrement();
        }
        return basicProcessor;
    }

    @Override
    public void init() throws Exception {

        log.info("[OmsJarContainer] start to init container(name={},jarPath={})", name, localJarFile.getPath());

        URL jarURL = localJarFile.toURI().toURL();

        // 创建类加载器
        this.containerClassLoader = new OhMyClassLoader(new URL[]{jarURL}, this.getClass().getClassLoader());

        // 获取资源文件
        URL propertiesURL = containerClassLoader.getResource(ContainerConstant.CONTAINER_PROPERTIES_FILE_NAME);
        URL springXmlURL = containerClassLoader.getResource(ContainerConstant.SPRING_CONTEXT_FILE_NAME);

        if (propertiesURL == null) {
            log.error("[OmsJarContainer] can't find {} in jar {}.", ContainerConstant.CONTAINER_PROPERTIES_FILE_NAME, localJarFile.getPath());
            throw new OmsWorkerException("invalid jar file");
        }
        if (springXmlURL == null) {
            log.error("[OmsJarContainer] can't find {} in jar {}.", ContainerConstant.SPRING_CONTEXT_FILE_NAME, localJarFile.getPath());
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

        // 创建 Spring IOC 容器（Spring配置文件需要填相对路径）
        this.container = new ClassPathXmlApplicationContext(new String[]{ContainerConstant.SPRING_CONTEXT_FILE_NAME}, false);
        this.container.setClassLoader(containerClassLoader);
        this.container.refresh();

        log.info("[OmsJarContainer] init container(name={},jarPath={}) successfully", name, localJarFile.getPath());
    }

    @Override
    public void destroy() throws Exception {

        // 没有其余引用时，才允许执行 destroy
        if (referenceCount.get() <= 0) {
            try {
                if (localJarFile.exists()) {
                    FileUtils.forceDelete(localJarFile);
                }
            }catch (Exception e) {
                log.warn("[OmsJarContainer-{}] delete jarFile({}) failed.", name, localJarFile.getPath(), e);
            }
            try {
                processorCache.clear();
                container.close();
                containerClassLoader.close();
                log.info("[OmsJarContainer-{}] container destroyed successfully", name);
            }catch (Exception e) {
                log.error("[OmsJarContainer-{}] container destroyed failed", name, e);
            }
            return;
        }

        log.warn("[OmsJarContainer-{}] container's reference count is {}, won't destroy now!", name, referenceCount.get());
    }

    @Override
    public String getName() {
        return name;
    }
    @Override
    public String getVersion() {
        return version;
    }
    @Override
    public Long getContainerId() {
        return containerId;
    }
    @Override
    public Long getDeployedTime() {
        return deployedTime;
    }

    @Override
    public void tryRelease() {

        log.debug("[OmsJarContainer-{}] tryRelease, current reference is {}.", name, referenceCount.get());
        // 需要满足的条件：引用计数器减为0 & 有更新的容器出现
        if (referenceCount.decrementAndGet() <= 0) {

            OmsContainer container = OmsContainerFactory.getContainer(name);
            if (container != this) {
                try {
                    destroy();
                }catch (Exception ignore) {
                }
            }
        }
    }
}
