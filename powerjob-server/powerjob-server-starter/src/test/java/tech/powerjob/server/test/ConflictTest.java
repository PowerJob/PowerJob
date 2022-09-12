package tech.powerjob.server.test;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.utils.CommonUtils;
import tech.powerjob.server.core.uid.SnowFlakeIdGenerator;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author Echo009
 * @since 2022/4/27
 */
@Slf4j
public class ConflictTest {

    @Test
    @SuppressWarnings("all")
    @SneakyThrows
    public void segmentLockMockTest() {

        final SnowFlakeIdGenerator snowFlakeIdGenerator = new SnowFlakeIdGenerator(0, 4);

        int len = CommonUtils.formatSize(1024) - 1;
        Map<Integer, Integer> matchCount = new TreeMap<>();
        int maxTime = 10000;
        int expectedMaxConflict = maxTime / len;

        for (int i = 0; i < maxTime; i++) {
            final long id = snowFlakeIdGenerator.nextId();
            // 这里模拟实际的请求间隔，新建任务在 1k 的 qps
            Thread.sleep(1);
//             int res = Long.valueOf(id).intValue() & len;
            int res = String.valueOf(Long.valueOf(id).intValue()).hashCode() & len;
            matchCount.merge(res, 1, Integer::sum);
        }
        final List<Map.Entry<Integer, Integer>> sorted = matchCount.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).collect(Collectors.toList());
        // 10w qps （这里包括实例状态上报的请求），最大冲突次数 407，假设每个请求处理耗时 10 ms，最大等待时长 4.07 s
        log.info("expectedMaxConflict: {},actualMaxConflict: {}", expectedMaxConflict, sorted.get(0).getValue());
        sorted.forEach(e -> {
            log.info("index: {} -> conflict: {}", e.getKey(), e.getValue());
        });

    }


}
