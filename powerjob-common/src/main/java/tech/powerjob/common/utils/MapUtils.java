package tech.powerjob.common.utils;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Map;

/**
 * MapUtils
 *
 * @author tjq
 * @since 2024/2/24
 */
public class MapUtils {

    public static <K> Long getLong(Map<? super K, ?> map, K key, Long defaultValue) {
        Long answer = getLong(map, key);
        if (answer == null) {
            answer = defaultValue;
        }

        return answer;
    }

    public static <K> long getLongValue(Map<? super K, ?> map, K key) {
        Long longObject = getLong(map, key);
        return longObject == null ? 0L : longObject;
    }

    public static <K> Long getLong(Map<? super K, ?> map, K key) {
        Number answer = getNumber(map, key);
        if (answer == null) {
            return null;
        } else {
            return answer instanceof Long ? (Long)answer : answer.longValue();
        }
    }

    public static <K> Number getNumber(Map<? super K, ?> map, K key) {
        if (map != null) {
            Object answer = map.get(key);
            if (answer != null) {
                if (answer instanceof Number) {
                    return (Number)answer;
                }

                if (answer instanceof String) {
                    try {
                        String text = (String)answer;
                        return NumberFormat.getInstance().parse(text);
                    } catch (ParseException var4) {
                    }
                }
            }
        }

        return null;
    }
}
