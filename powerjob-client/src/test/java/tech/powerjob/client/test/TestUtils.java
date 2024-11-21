package tech.powerjob.client.test;

import com.alibaba.fastjson.JSONObject;

/**
 * TestUtils
 *
 * @author tjq
 * @since 2024/11/21
 */
public class TestUtils {

    public static void output(Object v) {
        String str = JSONObject.toJSONString(v);
        System.out.println(str);
    }
}
