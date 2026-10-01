package tech.powerjob.server.common.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

public class AOPUtilsClassLoaderTest {
    private static final AtomicInteger EVALUATIONS = new AtomicInteger();

    public void target() { }

    public static int incrementAndReturnZero() {
        EVALUATIONS.incrementAndGet();
        return 0;
    }

    public static class TcclVisibleType { }

    @Test
    void resolvesApplicationTypesWhenForkJoinContextLoaderCannotSeeThem() throws Exception {
        Method method = getClass().getMethod("target");
        Integer result = ForkJoinPool.commonPool().submit(() -> {
            Thread thread = Thread.currentThread();
            ClassLoader previous = thread.getContextClassLoader();
            thread.setContextClassLoader(hidingApplicationTypes(previous));
            try {
                return AOPUtils.parseSpEl(method, new Object[0],
                        "T(tech.powerjob.common.enums.TimeExpressionType).CRON.getV()", Integer.class, -1);
            } finally {
                thread.setContextClassLoader(previous);
            }
        }).get();
        assertEquals(Integer.valueOf(2), result);
    }

    @Test
    void prefersTheContextLoaderWhenBothLoadersCanResolveTheType() throws Exception {
        String name = TcclVisibleType.class.getName();
        byte[] bytes;
        try (InputStream input = getClass().getResourceAsStream("/" + name.replace('.', '/') + ".class")) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = input.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
            bytes = output.toByteArray();
        }
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        ClassLoader contextLoader = new ClassLoader(previous) {
            @Override
            protected Class<?> loadClass(String requested, boolean resolve) throws ClassNotFoundException {
                if (!name.equals(requested)) {
                    return super.loadClass(requested, resolve);
                }
                Class<?> type = findLoadedClass(requested);
                return type == null ? defineClass(requested, bytes, 0, bytes.length) : type;
            }
        };
        Class<?> expected = contextLoader.loadClass(name);
        assertNotSame(TcclVisibleType.class, expected);
        thread.setContextClassLoader(contextLoader);
        try {
            assertSame(expected, AOPUtils.parseSpEl(getClass().getMethod("target"), new Object[0],
                    "T(" + name + ")", Class.class, null));
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Test
    void retriesTypeLookupWithoutRepeatingExpressionSideEffects() throws Exception {
        Thread thread = Thread.currentThread();
        ClassLoader previous = thread.getContextClassLoader();
        thread.setContextClassLoader(hidingApplicationTypes(previous));
        EVALUATIONS.set(0);
        try {
            assertEquals(Integer.valueOf(2), AOPUtils.parseSpEl(getClass().getMethod("target"), new Object[0],
                    "T(" + getClass().getName() + ").incrementAndReturnZero()"
                            + " + T(tech.powerjob.common.enums.TimeExpressionType).CRON.getV()", Integer.class, -1));
            assertEquals(1, EVALUATIONS.get());
        } finally {
            thread.setContextClassLoader(previous);
        }
    }

    @Test
    void preservesFallbackForInvalidExpressions() throws Exception {
        assertEquals(Integer.valueOf(-1), AOPUtils.parseSpEl(getClass().getMethod("target"),
                new Object[0], "T(no.such.ApplicationType)", Integer.class, -1));
    }

    private static ClassLoader hidingApplicationTypes(ClassLoader parent) {
        return new ClassLoader(parent) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.startsWith("tech.powerjob.common.")) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };
    }
}
