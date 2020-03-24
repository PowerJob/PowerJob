package com.github.kfcfans.oms;

import com.github.kfcfans.oms.worker.common.utils.NetUtils;
import org.junit.jupiter.api.Test;

/**
 * 测试工具类
 *
 * @author tjq
 * @since 2020/3/24
 */
public class UtilsTest {

    @Test
    public void testNetUtils() throws Exception {
        System.out.println("本机IP：" + NetUtils.getLocalHost());
    }
}
