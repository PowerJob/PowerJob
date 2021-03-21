package tech.powerjob.worker.common.utils;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.function.BiConsumer;

/**
 * LRU（Least Recently Used） 缓存
 * before v3.1.1 使用 LinkedHashMap，但存在修改时访问报错问题，改用 Guava
 *
 * @author tjq
 * @since 2020/4/8
 */
public class LRUCache<K, V> {

    private final Cache<K, V> innerCache;

    public LRUCache(int cacheSize) {
        innerCache = CacheBuilder.newBuilder()
                .concurrencyLevel(2)
                .maximumSize(cacheSize)
                .build();
    }

    public void forEach(BiConsumer<? super K, ? super V> action) {
        innerCache.asMap().forEach(action);
    }

    public V get(K key) {
        return innerCache.getIfPresent(key);
    }

    public void put(K key, V value) {
        innerCache.put(key, value);
    }
}
