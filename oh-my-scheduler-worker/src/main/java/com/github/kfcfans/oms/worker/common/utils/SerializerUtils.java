package com.github.kfcfans.oms.worker.common.utils;

import org.nustaq.serialization.FSTConfiguration;

import java.nio.charset.StandardCharsets;

/**
 * 序列化框架
 *
 * @author tjq
 * @since 2020/3/25
 */
public class SerializerUtils {

    private static FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    public static byte[] serialize(Object obj) {
        return conf.asByteArray(obj);
    }

    public static Object deSerialized(byte[] bytes) {
        return conf.asObject(bytes);
    }

    public static String toJSON(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return new String(serialize(object), StandardCharsets.UTF_8);
        }catch (Exception ignore) {
        }
        return null;
    }
}
