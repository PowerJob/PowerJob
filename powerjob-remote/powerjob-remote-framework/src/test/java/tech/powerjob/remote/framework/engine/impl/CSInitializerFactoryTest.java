package tech.powerjob.remote.framework.engine.impl;

import com.google.common.collect.Sets;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tech.powerjob.common.exception.PowerJobException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * CSInitializerFactoryTest
 *
 * @author tjq
 * @since 2022/12/31
 */
class CSInitializerFactoryTest {

    @Test
    void testBuildNormal() {
        CSInitializerFactory.build("TEST");
    }

    @Test
    void testNotFind() {
        Assertions.assertThrows(PowerJobException.class, () -> {
            CSInitializerFactory.build("omicron");
        });
    }
}