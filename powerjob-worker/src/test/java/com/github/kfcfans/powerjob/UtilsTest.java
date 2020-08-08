package com.github.kfcfans.powerjob;

import com.github.kfcfans.powerjob.worker.common.utils.SystemInfoUtils;
import org.junit.jupiter.api.Test;

/**
 * 测试工具类
 *
 * @author tjq
 * @since 2020/3/24
 */
public class UtilsTest {


    @Test
    public void testSystemInfoUtils() {
        System.out.println(SystemInfoUtils.getSystemMetrics());
    }

}
