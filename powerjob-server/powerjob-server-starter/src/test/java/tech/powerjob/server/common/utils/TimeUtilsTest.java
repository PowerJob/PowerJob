package tech.powerjob.server.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 时间工具 Test
 *
 * @author tjq
 * @since 2020/5/19
 */
@Slf4j
class TimeUtilsTest {

    @Test
    void calculateWeek() {
        int weekOf20230211 = TimeUtils.calculateWeek(2023, 2, 11);
        log.info("[TimeUtilsTest] weekOf20230211: {}", weekOf20230211);
        assert weekOf20230211 == 6;

        int weekOf20230212 = TimeUtils.calculateWeek(2023, 2, 12);
        log.info("[TimeUtilsTest] weekOf20230211: {}", weekOf20230212);
        assert weekOf20230212 == 7;

        int weekOf20230401 = TimeUtils.calculateWeek(2023, 4, 1);
        log.info("[TimeUtilsTest] weekOf20230401: {}", weekOf20230401);
        assert weekOf20230401 == 6;


        // 618
        int weekOf20230618 = TimeUtils.calculateWeek(2023, 6, 18);
        log.info("[TimeUtilsTest] weekOf20230618: {}", weekOf20230618);
        assert weekOf20230618 == 7;

        // 双十一
        int weekOf20231111 = TimeUtils.calculateWeek(2023, 11, 11);
        log.info("[TimeUtilsTest] weekOf20231111: {}", weekOf20231111);
        assert weekOf20231111 == 6;

        // 我发现所有我熟悉的日子都是周末，神器啊....

        int weekOf20230723 = TimeUtils.calculateWeek(2023, 7, 23);
        log.info("[TimeUtilsTest] weekOf20230723: {}", weekOf20230723);
        assert weekOf20230723 == 7;

        int weekOf20230218 = TimeUtils.calculateWeek(2023, 2, 18);
        log.info("[TimeUtilsTest] weekOf20230218: {}", weekOf20230218);
        assert weekOf20230218 == 6;

    }
}