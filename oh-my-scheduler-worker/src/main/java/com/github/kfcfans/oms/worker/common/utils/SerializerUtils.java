package com.github.kfcfans.oms.worker.common.utils;


import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.util.Pool;

/**
 * 序列化器
 *
 * @author tjq
 * @since 2020/3/25
 */
public class SerializerUtils {

    private static final int DEFAULT_CAPACITY = Runtime.getRuntime().availableProcessors();
    private static final Pool<Kryo> kryoPool = new Pool<Kryo>(true, false, DEFAULT_CAPACITY) {
        @Override
        protected Kryo create() {

            Kryo kryo = new Kryo();
            // 关闭序列化注册，会导致性能些许下降，但在分布式环境中，注册类生成ID不一致会导致错误
            kryo.setRegistrationRequired(false);
            // 支持循环引用，也会导致性能些许下降 T_T
            kryo.setReferences(true);
            return kryo;
        }
    };

    public static byte[] serialize(Object obj) {

        Kryo kryo = kryoPool.obtain();

        // 使用 Output 对象池会导致序列化重复的错误（getBuffer返回了Output对象的buffer引用）
        try (Output opt = new Output(1024, -1)) {
            kryo.writeClassAndObject(opt, obj);
            opt.flush();
            return opt.getBuffer();
        }finally {
            kryoPool.free(kryo);
        }
    }

    public static Object deSerialized(byte[] buffer) {
        Kryo kryo = kryoPool.obtain();
        try {
            return kryo.readClassAndObject(new Input(buffer));
        }finally {
            kryoPool.free(kryo);
        }
    }

}
