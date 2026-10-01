package tech.powerjob.remote.framework.engine.impl;

import org.junit.jupiter.api.Test;
import tech.powerjob.remote.framework.test.TestCSInitializer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class PowerJobRemoteEngineCloseTest {

    @Test
    void closesSafelyBeforeInitialization() {
        PowerJobRemoteEngine engine = new PowerJobRemoteEngine();
        assertDoesNotThrow(engine::close);
        assertDoesNotThrow(engine::close);
    }

    @Test
    void stillDelegatesCloseToAnInitializedEngine() throws Exception {
        PowerJobRemoteEngine engine = new PowerJobRemoteEngine();
        AtomicInteger closes = new AtomicInteger();
        initializer(engine, new TestCSInitializer() {
            @Override
            public void close() {
                closes.incrementAndGet();
            }
        });
        engine.close();
        assertEquals(1, closes.get());
    }

    @Test
    void preservesTheInitializerCloseFailure() throws Exception {
        PowerJobRemoteEngine engine = new PowerJobRemoteEngine();
        IOException failure = new IOException("close failed");
        initializer(engine, new TestCSInitializer() {
            @Override
            public void close() throws IOException {
                throw failure;
            }
        });
        assertSame(failure, assertThrows(IOException.class, engine::close));
    }

    private static void initializer(PowerJobRemoteEngine engine, TestCSInitializer initializer) throws Exception {
        Field field = PowerJobRemoteEngine.class.getDeclaredField("csInitializer");
        field.setAccessible(true);
        field.set(engine, initializer);
    }
}
