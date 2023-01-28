package tech.powerjob.remote.akka;

import akka.serialization.JSerializer;
import tech.powerjob.common.serialize.SerializerUtils;

/**
 * Using custom serializers for akka-remote
 * https://doc.akka.io/docs/akka/current/serialization.html
 *
 * @author tjq
 * @since 2021/3/21
 */
public class PowerAkkaSerializer extends JSerializer {

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        return SerializerUtils.deSerialized(bytes);
    }

    @Override
    public int identifier() {
        return 277777;
    }

    @Override
    public byte[] toBinary(Object o) {
        return SerializerUtils.serialize(o);
    }

    @Override
    public boolean includeManifest() {
        return false;
    }
}

