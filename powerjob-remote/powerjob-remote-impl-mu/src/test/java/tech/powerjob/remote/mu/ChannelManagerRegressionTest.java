package tech.powerjob.remote.mu;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import tech.powerjob.remote.framework.base.Address;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;

class ChannelManagerRegressionTest {

    @Test
    void closingAnOldChannelPreservesItsReplacement() {
        ChannelManager manager = new ChannelManager();
        Address address = new Address().setHost("127.0.0.1").setPort(19003);
        EmbeddedChannel old = new EmbeddedChannel();
        EmbeddedChannel replacement = new EmbeddedChannel();
        try {
            manager.registerWorkerChannel(address, old);
            manager.registerWorkerChannel(address, replacement);
            old.close();
            assertSame(replacement, manager.getWorkerChannel(address));
            replacement.close();
            assertNull(manager.getWorkerChannel(address));
        } finally {
            old.finishAndReleaseAll();
            replacement.finishAndReleaseAll();
        }
    }

    @Test
    void anInvalidResponseCompletesExceptionallyAndReleasesPendingState() throws Exception {
        ChannelManager manager = new ChannelManager();
        CompletableFuture<Object> future = new CompletableFuture<>();
        manager.registerPendingRequest("invalid", future, Integer.class);
        assertDoesNotThrow(() -> manager.completePendingRequest("invalid", "invalid-integer"));
        assertTrue(future.isCompletedExceptionally());
        assertTrue(assertThrows(CompletionException.class, future::join).getCause() instanceof IllegalArgumentException);
        assertNoPendingState(manager);
    }

    @Test
    void validConversionsAndNullResponsesRetainTheirBehavior() throws Exception {
        ChannelManager manager = new ChannelManager();
        CompletableFuture<Object> converted = new CompletableFuture<>();
        manager.registerPendingRequest("converted", converted, Integer.class);
        manager.completePendingRequest("converted", "123");
        assertEquals(123, converted.join());

        CompletableFuture<Object> empty = new CompletableFuture<>();
        manager.registerPendingRequest("empty", empty, Integer.class);
        manager.completePendingRequest("empty", null);
        assertNull(empty.join());
        assertNoPendingState(manager);
    }

    private static void assertNoPendingState(ChannelManager manager) throws Exception {
        for (String name : new String[]{"pendingRequests", "requestResponseTypes"}) {
            Field field = ChannelManager.class.getDeclaredField(name);
            field.setAccessible(true);
            assertTrue(((Map<?, ?>) field.get(manager)).isEmpty(), name);
        }
    }
}
