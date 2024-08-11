package tech.powerjob.common.utils;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.exception.PowerJobException;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CommonUtilsTest
 *
 * @author tjq
 * @since 2024/8/11
 */
class CommonUtilsTest {

    @Test
    void testRequireNonNull() {

        assertThrowsExactly(PowerJobException.class, () -> CommonUtils.requireNonNull(null, "NULL_OBJ"));
        assertThrowsExactly(PowerJobException.class, () -> CommonUtils.requireNonNull("", "EMPTY_STR"));
        assertThrowsExactly(PowerJobException.class, () -> CommonUtils.requireNonNull(Lists.newArrayList(), "EMPTY_COLLECTION"));
        assertThrowsExactly(PowerJobException.class, () -> CommonUtils.requireNonNull(Collections.emptyMap(), "EMPTY_MAP"));

        Map<String, Object> map = Maps.newHashMap();
        map.put("1", 1);

        CommonUtils.requireNonNull(1, "NORMAL");
        CommonUtils.requireNonNull("1", "NORMAL");
        CommonUtils.requireNonNull(Lists.newArrayList("1"), "NORMAL");
        CommonUtils.requireNonNull(map, "NORMAL");

    }

}