package com.github.kfcfans.oms.server.service;

/**
 * 唯一ID生成服务
 *
 * @author tjq
 * @since 2020/4/6
 */
public class IdGenerateService {

    public static Long allocate() {
        // TODO：换成合适的分布式ID生成算法
        return System.currentTimeMillis();
    }

}
