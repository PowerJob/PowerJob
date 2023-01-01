package tech.powerjob.remote.framework.utils;

import org.apache.commons.lang3.ArrayUtils;
import tech.powerjob.common.PowerSerializable;

import java.util.Optional;

/**
 * RemoteUtils
 *
 * @author tjq
 * @since 2023/1/1
 */
public class RemoteUtils {

    public static Optional<Class<?>> findPowerSerialize(Class<?>[] parameterTypes) {

        if (ArrayUtils.isEmpty(parameterTypes)) {
            return Optional.empty();
        }

        for (Class<?> clz : parameterTypes) {
            final Class<?>[] interfaces = clz.getInterfaces();
            if (ArrayUtils.isEmpty(interfaces)) {
                continue;
            }

            if (PowerSerializable.class.isAssignableFrom(clz)) {
                return Optional.of(clz);
            }
        }
        return Optional.empty();
    }

}
