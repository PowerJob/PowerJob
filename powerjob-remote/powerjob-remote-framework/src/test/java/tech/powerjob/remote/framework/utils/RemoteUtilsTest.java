package tech.powerjob.remote.framework.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.model.AlarmConfig;
import tech.powerjob.common.request.ServerScheduleJobReq;

import java.util.Optional;


/**
 * RemoteUtilsTest
 *
 * @author tjq
 * @since 2023/1/1
 */
@Slf4j
class RemoteUtilsTest {

    @Test
    void findPowerSerialize() {

        Class<?>[] contains = {AlarmConfig.class, ServerScheduleJobReq.class};
        Class<?>[] notContains = {AlarmConfig.class};

        final Optional<Class<?>> notContainsResult = RemoteUtils.findPowerSerialize(notContains);
        log.info("[RemoteUtilsTest] notContainsResult: {}", notContainsResult);
        final Optional<Class<?>> containsResult = RemoteUtils.findPowerSerialize(contains);
        log.info("[RemoteUtilsTest] containsResult: {}", containsResult);

        assert !notContainsResult.isPresent();
        assert containsResult.isPresent();

    }
}