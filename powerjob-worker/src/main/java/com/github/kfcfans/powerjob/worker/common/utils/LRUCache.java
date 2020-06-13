package com.github.kfcfans.powerjob.worker.common.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU（Least Recently Used） 缓存
 *
 * @author tjq
 * @since 2020/4/8
 */
public class LRUCache<K, V> extends LinkedHashMap<K, V> {

    private final int cacheSize;

    public LRUCache(int cacheSize) {
        super((int) Math.ceil(cacheSize / 0.75) + 1, 0.75f, false);
        this.cacheSize = cacheSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K,V> eldest) {
        // 超过阈值时返回true，进行LRU淘汰
        return size() > cacheSize;
    }
}
