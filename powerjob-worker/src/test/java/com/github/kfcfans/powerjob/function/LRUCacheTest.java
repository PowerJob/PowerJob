package com.github.kfcfans.powerjob.function;

import com.github.kfcfans.powerjob.worker.common.utils.LRUCache;
import org.junit.jupiter.api.Test;

/**
 * LRU cache test
 *
 * @author tjq
 * @since 2020/6/26
 */
public class LRUCacheTest {

    @Test
    public void testCache() {
        LRUCache<Long, String> cache = new LRUCache<>(10);
        for (long i = 0; i < 100; i++) {
            cache.put(i, "STR:" + i);
        }
        cache.forEach((x, y) -> System.out.println("key:" + x));
    }

}
