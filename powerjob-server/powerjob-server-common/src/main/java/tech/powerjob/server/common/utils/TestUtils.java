package tech.powerjob.server.common.utils;

import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 开发团队专用测试工具
 *
 * @author tjq
 * @since 2023/7/31
 */
public class TestUtils {

    private static final String TEST_CONFIG_NAME = "/.powerjob_test";

    public static final String KEY_PHONE_NUMBER = "phone";

    public static final String KEY_MONGO_URI = "mongoUri";

    /**
     * 获取本地的测试配置，主要用于存放一些密钥
     * @return 测试配置
     */
    public static Map<String, Object> fetchTestConfig() {
        try {
            // 后续本地测试，密钥相关的内容统一存入 .powerjob_test 中，方便管理
            String content = FileUtils.readFileToString(new File(System.getProperty("user.home").concat(TEST_CONFIG_NAME)), StandardCharsets.UTF_8);
            if (StringUtils.isNotEmpty(content)) {
                return JSONObject.parseObject(content);
            }
        } catch (Exception ignore) {
        }
        return Maps.newHashMap();
    }

}
