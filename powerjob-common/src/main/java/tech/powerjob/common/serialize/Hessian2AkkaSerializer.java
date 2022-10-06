package tech.powerjob.common.serialize;

import akka.serialization.JSerializer;
import tech.powerjob.common.serialize.hessian2.Hessian2ObjectInput;
import tech.powerjob.common.serialize.hessian2.Hessian2ObjectOutput;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;

/**
 * Using custom serializers for akka-remote
 * Kryo只支持java
 * <a href="https://github.com/hessian-group">跨语言序列化反序列化框架</a>
 * 为了方便集成跨语言版本的work 比如go或者csharp版本的work
 *
 * @author zdyu
 * @since 2022/10/6
 */
public class Hessian2AkkaSerializer extends JSerializer implements Serializable {

    @Override
    public Object fromBinaryJava(byte[] bytes, Class<?> manifest) {
        try (InputStream inputStream = new ByteArrayInputStream(bytes);
             Hessian2ObjectInput input = new Hessian2ObjectInput(inputStream);
        ) {
            return input.readObject();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int identifier() {
        return 277778;
    }

    @Override
    public byte[] toBinary(Object o) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             Hessian2ObjectOutput output = new Hessian2ObjectOutput(outputStream);
        ) {
            output.writeObject(o);
            output.flushBuffer();
            return outputStream.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public boolean includeManifest() {
        return false;
    }
}
