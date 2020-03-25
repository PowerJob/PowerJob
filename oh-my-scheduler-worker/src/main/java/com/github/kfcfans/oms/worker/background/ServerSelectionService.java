package com.github.kfcfans.oms.worker.background;

import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 选择服务器
 *
 * @author tjq
 * @since 2020/3/25
 */
public class ServerSelectionService {

    private static String appName;
    private static List<String> allServerAddress;
    private static String defaultServerAddress;


    /**
     * 获取默认服务器，同一个 appName 一定对应同一台服务器
     */
    private static String getDefaultServer() {
        if (!StringUtils.isEmpty(defaultServerAddress)) {
            return defaultServerAddress;
        }

        Long index = letter2Num(appName);
        defaultServerAddress = allServerAddress.get(index.intValue() % allServerAddress.size());
        return defaultServerAddress;
    }

    private static Long letter2Num(String str) {
        if (StringUtils.isEmpty(str)) {
            return 0L;
        }
        AtomicLong res = new AtomicLong(0);
        str.chars().forEach(res::addAndGet);
        return res.get();
    }

}
