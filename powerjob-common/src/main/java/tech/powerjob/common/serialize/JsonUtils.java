package tech.powerjob.common.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.exception.PowerJobException;
import org.apache.commons.lang3.exception.ExceptionUtils;

import java.io.IOException;

/**
 * JSON工具类
 *
 * @author tjq
 * @since 2020/4/16
 */
@Slf4j
public class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
    }

    private JsonUtils(){

    }

    public static String toJSONString(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        }catch (Exception ignore) {
        }
        return null;
    }

    public static String toJSONStringUnsafe(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        }catch (Exception e) {
            throw new PowerJobException(e);
        }
    }

    public static byte[] toBytes(Object obj) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        }catch (Exception ignore) {
        }
        return null;
    }

    public static <T> T parseObject(String json, Class<T> clz) throws JsonProcessingException {
        return OBJECT_MAPPER.readValue(json, clz);
    }

    public static <T> T parseObject(byte[] b, Class<T> clz) throws IOException {
        return OBJECT_MAPPER.readValue(b, clz);
    }

    public static <T> T parseObject(byte[] b, TypeReference<T> typeReference) throws IOException {
        return OBJECT_MAPPER.readValue(b, typeReference);
    }

    public static <T> T parseObject(String json, TypeReference<T> typeReference) throws IOException {
        return OBJECT_MAPPER.readValue(json, typeReference);
    }

    public static <T> T parseObjectIgnoreException(String json, Class<T> clz) {
        try {
            return OBJECT_MAPPER.readValue(json, clz);
        }catch (Exception e) {
            log.error("unable to parse json string to object,current string:{}",json,e);
            return null;
        }

    }

    public static <T> T parseObjectUnsafe(String json, Class<T> clz) {
        try {
            return OBJECT_MAPPER.readValue(json, clz);
        }catch (Exception e) {
            ExceptionUtils.rethrow(e);
        }
        throw new PowerJobException("impossible");
    }
}
