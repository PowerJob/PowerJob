package tech.powerjob.remote.framework.engine.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.reflections.Reflections;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.remote.framework.cs.CSInitializer;

import java.util.Set;

/**
 * build CSInitializer
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
class CSInitializerFactory {

    static CSInitializer build(String targetType) {

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
}
