package tech.powerjob.server.test;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.TimeZone;
import java.util.stream.Collectors;

/**
 * 工具类测试
 *
 * @author tjq
 * @since 2020/4/3
 */
public class UtilsTest {

    @Test
    public void normalTest() {
        String s = "000000000111010000001100000000010110100110100000000001000000000000";
        System.out.println(s.length());
    }

    @Test
    public void testTZ() {
        System.out.println(TimeZone.getDefault());
    }

    @Test
    public void testStringUtils() {
        String goodAppName = "powerjob-server";
        String appName = "powerjob-server ";
        System.out.println(StringUtils.containsWhitespace(goodAppName));
        System.out.println(StringUtils.containsWhitespace(appName));
    }

    @Test
    public void filterTest() {
        List<String> test = Lists.newArrayList("A", "B", null, "C", null);
        List<String> list = test.stream().filter(Objects::nonNull).collect(Collectors.toList());
        System.out.println(list);
    }
}
