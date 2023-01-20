package tech.powerjob.worker.container;

import tech.powerjob.common.ContainerConstant;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.apache.commons.lang3.StringUtils;

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

    private final Map<String, BasicProcessor> processorCache = Maps.newConcurrentMap();

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
                log.error("[OmsJarContainer-{}] can't find class: {} in container.", containerId, className);
                return null;
            }

            // 先尝试从 Spring IOC 容器加载
            try {
                return (BasicProcessor) container.getBean(targetClass);
            } catch (BeansException be) {
                log.warn("[OmsJarContainer-{}] load instance from spring container failed, try to build instance directly.", containerId);
            } catch (ClassCastException cce) {
                log.error("[OmsJarContainer-{}] {} should implements the Processor interface!", containerId, className);
                return null;
            } catch (Exception e) {
                log.error("[OmsJarContainer-{}] get bean failed for {}.", containerId, className, e);
                return null;
            }

            // 直接实例化
            try {
                Object obj = targetClass.getDeclaredConstructor().newInstance();
                return (BasicProcessor) obj;
            } catch (Exception e) {
                log.error("[OmsJarContainer-{}] load {} failed", containerId, className, e);
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

        log.info("[OmsJarContainer-{}] start to init container(name={},jarPath={})", containerId, name, localJarFile.getPath());

        URL jarURL = localJarFile.toURI().toURL();

        // 创建类加载器（父类加载为 Worker 的类加载）
        this.containerClassLoader = new OhMyClassLoader(new URL[]{jarURL}, this.getClass().getClassLoader());

        // 解析 Properties
        Properties properties = new Properties();
        try (InputStream propertiesURLStream = containerClassLoader.getResourceAsStream(ContainerConstant.CONTAINER_PROPERTIES_FILE_NAME)) {

            if (propertiesURLStream == null) {
                log.error("[OmsJarContainer-{}] can't find {} in jar {}.", containerId, ContainerConstant.CONTAINER_PROPERTIES_FILE_NAME, localJarFile.getPath());
                throw new PowerJobException("invalid jar file because of no " + ContainerConstant.CONTAINER_PROPERTIES_FILE_NAME);
            }

            properties.load(propertiesURLStream);
            log.info("[OmsJarContainer-{}] load container properties successfully: {}", containerId, properties);
        }
        String packageName = properties.getProperty(ContainerConstant.CONTAINER_PACKAGE_NAME_KEY);
        if (StringUtils.isEmpty(packageName)) {
            log.error("[OmsJarContainer-{}] get package name failed, developer should't modify the properties file!", containerId);
            throw new PowerJobException("invalid jar file");
        }

        // 加载用户类
        containerClassLoader.load(packageName);

        // 创建 Spring IOC 容器（Spring配置文件需要填相对路径）
        // 需要切换线程上下文类加载器以加载 JDBC 类驱动（SPI）
        ClassLoader oldCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(containerClassLoader);
        try {
            this.container = new ClassPathXmlApplicationContext(new String[]{ContainerConstant.SPRING_CONTEXT_FILE_NAME}, false);
            this.container.setClassLoader(containerClassLoader);
            this.container.refresh();
        }finally {
            Thread.currentThread().setContextClassLoader(oldCL);
        }

        log.info("[OmsJarContainer-{}] init container(name={},jarPath={}) successfully", containerId, name, localJarFile.getPath());
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
                log.warn("[OmsJarContainer-{}] delete jarFile({}) failed.", containerId, localJarFile.getPath(), e);
            }
            try {
                processorCache.clear();
                container.close();
                containerClassLoader.close();
                log.info("[OmsJarContainer-{}] container destroyed successfully", containerId);
            }catch (Exception e) {
                log.error("[OmsJarContainer-{}] container destroyed failed", containerId, e);
            }
            return;
        }

        log.warn("[OmsJarContainer-{}] container's reference count is {}, won't destroy now!", containerId, referenceCount.get());
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
    public OhMyClassLoader getContainerClassLoader() {
        return containerClassLoader;
    }

    @Override
    public void tryRelease() {

        log.debug("[OmsJarContainer-{}] tryRelease, current reference is {}.", containerId, referenceCount.get());
        // 需要满足的条件：引用计数器减为0 & 有更新的容器出现
        if (referenceCount.decrementAndGet() <= 0) {

            OmsContainer container = OmsContainerFactory.fetchContainer(containerId, null);
            if (container != this) {
                try {
                    destroy();
                }catch (Exception ignore) {
                }
            }
        }
    }
}
