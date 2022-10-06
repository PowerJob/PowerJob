package tech.powerjob.common.serialize.hessian2;

import com.alibaba.com.caucho.hessian.io.SerializerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zdyu
 * @since 2022/10/6
 */
public final class DefaultHessian2FactoryInitializer {
    private static final Map<ClassLoader, SerializerFactory> CL_2_SERIALIZER_FACTORY = new ConcurrentHashMap<>();
    private static volatile SerializerFactory SYSTEM_SERIALIZER_FACTORY;
    private static final DefaultHessian2FactoryInitializer INSTANCE = new DefaultHessian2FactoryInitializer();

    private DefaultHessian2FactoryInitializer() {
    }

    public static DefaultHessian2FactoryInitializer getInstance() {
        return INSTANCE;
    }

    public SerializerFactory getSerializerFactory() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            // system classloader
            if (SYSTEM_SERIALIZER_FACTORY == null) {
                synchronized (DefaultHessian2FactoryInitializer.class) {
                    if (SYSTEM_SERIALIZER_FACTORY == null) {
                        SYSTEM_SERIALIZER_FACTORY = createSerializerFactory();
                    }
                }
            }
            return SYSTEM_SERIALIZER_FACTORY;
        }

        if (!CL_2_SERIALIZER_FACTORY.containsKey(classLoader)) {
            synchronized (DefaultHessian2FactoryInitializer.class) {
                if (!CL_2_SERIALIZER_FACTORY.containsKey(classLoader)) {
                    SerializerFactory serializerFactory = createSerializerFactory();
                    CL_2_SERIALIZER_FACTORY.put(classLoader, serializerFactory);
                    return serializerFactory;
                }
            }
        }
        return CL_2_SERIALIZER_FACTORY.get(classLoader);
    }

    private SerializerFactory createSerializerFactory() {
        Hessian2SerializerFactory hessian2SerializerFactory = new Hessian2SerializerFactory();
        hessian2SerializerFactory.setAllowNonSerializable(false);
        hessian2SerializerFactory.getClassFactory().allow("tech.powerjob.*");
        return hessian2SerializerFactory;
    }
}
