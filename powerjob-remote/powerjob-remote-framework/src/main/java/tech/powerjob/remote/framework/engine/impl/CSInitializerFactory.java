package tech.powerjob.remote.framework.engine.impl;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.reflections.Reflections;
import tech.powerjob.common.OmsConstant;
import tech.powerjob.common.exception.PowerJobException;
import tech.powerjob.remote.framework.cs.CSInitializer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * build CSInitializer
 *
 * @author tjq
 * @since 2022/12/31
 */
@Slf4j
class CSInitializerFactory {

    static List<CSInitializer> build(Set<String> types) {

        Reflections reflections = new Reflections(OmsConstant.PACKAGE);
        Set<Class<? extends CSInitializer>> cSInitializerClzSet = reflections.getSubTypesOf(CSInitializer.class);

        log.info("[CSInitializerFactory] scan subTypeOf CSInitializer: {}", cSInitializerClzSet);

        List<CSInitializer> ret = Lists.newArrayList();

        cSInitializerClzSet.forEach(clz -> {
            try {
                CSInitializer csInitializer = clz.getDeclaredConstructor().newInstance();
                String type = csInitializer.type();
                log.info("[CSInitializerFactory] new instance for CSInitializer[{}] successfully, type={}, object: {}", clz, type, csInitializer);
                if (types.contains(type)) {
                    ret.add(csInitializer);
                }
            } catch (Exception e) {
                log.error("[CSInitializerFactory] new instance for CSInitializer[{}] failed, maybe you should provide a non-parameter constructor", clz);
                ExceptionUtils.rethrow(e);
            }
        });

        Set<String> loadTypes = ret.stream().map(CSInitializer::type).collect(Collectors.toSet());
        log.info("[CSInitializerFactory] final load types: {}", loadTypes);

        if (types.size() == ret.size()) {
            return ret;
        }

        Set<String> remainTypes = Sets.newHashSet(types);
        remainTypes.removeAll(loadTypes);

        throw new PowerJobException(String.format("can't load these CSInitializer[%s], ensure your package name start with 'tech.powerjob'!", remainTypes));
    }
}
