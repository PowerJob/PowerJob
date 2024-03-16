package tech.powerjob.remote.framework.engine.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.reflections.Reflections;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.enums.Protocol;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.remote.framework.cs.CSInitializer;

import java.util.Optional;
import java.util.Set;

/**
 * build CSInitializer
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
class CSInitializerFactory {

    private static final String OFFICIAL_HTTP_CS_INITIALIZER = "tech.powerjob.remote.http.HttpVertxCSInitializer";
    /**
     * 未来底层框架摆脱 vertx 时可能会用这个 classname，or 开发者自己实现的 http 协议也可以用这个 classname，总之预留战未来
     */
    private static final String OFFICIAL_HTTP_CS_INITIALIZER2 = "tech.powerjob.remote.http.HttpCSInitializer";
    private static final String OFFICIAL_AKKA_CS_INITIALIZER = "tech.powerjob.remote.akka.AkkaCSInitializer";

    private static final String EXTEND_CS_INITIALIZER_PATTERN = "tech.powerjob.remote.%s.CSInitializer";

    static CSInitializer build(String targetType) {

        CSInitializer officialCSInitializer = tryLoadCSInitializerByClassName(targetType);
        if (officialCSInitializer != null) {
            return officialCSInitializer;
        }

        log.info("[CSInitializerFactory] try load CSInitializerFactory by name failed, start to use Reflections!");

        // JAVA SPI 机制太笨了，短期内继续保留 Reflections 官网下高版本兼容性
        Reflections reflections = new Reflections(OmsConstant.PACKAGE);
        Set<Class<? extends CSInitializer>> cSInitializerClzSet = reflections.getSubTypesOf(CSInitializer.class);

        log.info("[CSInitializerFactory] scan subTypeOf CSInitializer: {}", cSInitializerClzSet);

        for (Class<? extends CSInitializer> clz : cSInitializerClzSet) {
            try {
                CSInitializer csInitializer = clz.getDeclaredConstructor().newInstance();
                String type = csInitializer.type();
                log.info("[CSInitializerFactory] new instance for CSInitializer[{}] successfully, type={}, object: {}", clz, type, csInitializer);
                if (targetType.equalsIgnoreCase(type)) {
                    return csInitializer;
                }
            } catch (Exception e) {
                log.error("[CSInitializerFactory] new instance for CSInitializer[{}] failed, maybe you should provide a non-parameter constructor", clz);
                ExceptionUtils.rethrow(e);
            }
        }

        throw new PowerJobException(String.format("can't load CSInitializer[%s], ensure your package name start with 'tech.powerjob' and import the dependencies!", targetType));
    }

    /**
     * 官方组件直接使用固定类名尝试加载，确保 reflections 不兼容情况下，至少能使用官方通讯协议
     * @param targetType 协议类型
     * @return CSInitializer
     */
    private static CSInitializer tryLoadCSInitializerByClassName(String targetType) {

        if (Protocol.HTTP.name().equalsIgnoreCase(targetType)) {
            Optional<CSInitializer> httpCsIOpt = tryLoadCSInitializerByClzName(OFFICIAL_HTTP_CS_INITIALIZER);
            if (httpCsIOpt.isPresent()) {
                return httpCsIOpt.get();
            }
            Optional<CSInitializer> httpCsIOpt2 = tryLoadCSInitializerByClzName(OFFICIAL_HTTP_CS_INITIALIZER2);
            if (httpCsIOpt2.isPresent()) {
                return httpCsIOpt2.get();
            }
        }

        if (Protocol.AKKA.name().equalsIgnoreCase(targetType)) {
            Optional<CSInitializer> akkaCSIOpt = tryLoadCSInitializerByClzName(OFFICIAL_AKKA_CS_INITIALIZER);
            if (akkaCSIOpt.isPresent()) {
                return akkaCSIOpt.get();
            }
        }

        // 尝试加载按规范命名的处理器，比如使用方自定义了 http2 协议，将其类名定为 tech.powerjob.remote.http2.CSInitializer 依然可确保在 Reflections 不可用的情况下完成加载
        String clz = String.format(EXTEND_CS_INITIALIZER_PATTERN, targetType);
        Optional<CSInitializer> extOpt = tryLoadCSInitializerByClzName(clz);
        return extOpt.orElse(null);

    }


    private static Optional<CSInitializer> tryLoadCSInitializerByClzName(String clzName) {
        try {
            log.info("[CSInitializerFactory] try to load CSInitializer by classname: {}", clzName);
            Class<?> clz = Class.forName(clzName);
            CSInitializer o = (CSInitializer) clz.getDeclaredConstructor().newInstance();
            log.info("[CSInitializerFactory] load CSInitializer[{}] successfully, obj: {}", clzName, o);
            return Optional.of(o);
        } catch (ClassNotFoundException ce) {
            log.warn("[CSInitializerFactory] load CSInitializer by classname[{}] failed due to ClassNotFound: {}", clzName, ExceptionUtils.getMessage(ce));
        } catch (Exception e) {
            log.warn("[CSInitializerFactory] load CSInitializer by classname[{}] failed.", clzName, e);
        }
        return Optional.empty();
    }
}
