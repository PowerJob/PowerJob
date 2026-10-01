package tech.powerjob.remote.mu;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.common.serialize.JsonUtils;
import tech.powerjob.remote.framework.base.Address;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Channel manager for maintaining worker address to channel mapping
 * Supports both tell and ask modes for reverse communication
 *
 * @author claude
 * @since 2025/1/1
 */
@Slf4j
public class ChannelManager {

    private final ConcurrentMap<String, Channel> workerChannels = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<Object>> pendingRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Class<?>> requestResponseTypes = new ConcurrentHashMap<>();

    /**
     * Register a worker channel
     * @param workerAddress worker address
     * @param channel Netty channel
     */
    public void registerWorkerChannel(Address workerAddress, Channel channel) {
        String key = workerAddress.getHost() + ":" + workerAddress.getPort();
        workerChannels.put(key, channel);
        log.info("[ChannelManager] Registered worker channel: {}", key);
        
        // Remove channel when it becomes inactive
        channel.closeFuture().addListener(future -> {
            workerChannels.remove(key, channel);
            log.info("[ChannelManager] Removed inactive worker channel: {}", key);
        });
    }

    /**
     * Get channel for worker
     * @param workerAddress worker address
     * @return Channel or null if not found
     */
    public Channel getWorkerChannel(Address workerAddress) {
        String key = workerAddress.getHost() + ":" + workerAddress.getPort();
        return workerChannels.get(key);
    }

    /**
     * Store pending request for ask mode
     * @param requestId request ID
     * @param future future to complete when response received
     * @param responseType expected response type
     */
    public void registerPendingRequest(String requestId, CompletableFuture<Object> future, Class<?> responseType) {
        pendingRequests.put(requestId, future);
        requestResponseTypes.put(requestId, responseType);
    }

    /**
     * Complete pending request with response
     * @param requestId request ID
     * @param response response object
     */
    public void completePendingRequest(String requestId, Object response) {
        CompletableFuture<Object> future = pendingRequests.remove(requestId);
        Class<?> responseType = requestResponseTypes.remove(requestId);
        
        if (future != null) {
            try {
                future.complete(convertResponse(response, responseType));
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        } else {
            log.warn("[ChannelManager] No pending request found for ID: {}", requestId);
        }
    }

    /**
     * Complete pending request with exception
     * @param requestId request ID
     * @param exception exception
     */
    public void completePendingRequestExceptionally(String requestId, Throwable exception) {
        CompletableFuture<Object> future = pendingRequests.remove(requestId);
        requestResponseTypes.remove(requestId); // Clean up response type mapping
        if (future != null) {
            future.completeExceptionally(exception);
        } else {
            log.warn("[ChannelManager] No pending request found for ID: {}", requestId);
        }
    }

    /**
     * Remove pending request (timeout cleanup)
     * @param requestId request ID
     */
    public void removePendingRequest(String requestId) {
        pendingRequests.remove(requestId);
        requestResponseTypes.remove(requestId);
    }

    /**
     * Convert response to expected type
     * @param response raw response object
     * @param responseType expected response type
     * @return converted response
     */
    private Object convertResponse(Object response, Class<?> responseType) {
        if (response == null || responseType == null) {
            return response;
        }
        
        if (responseType.isInstance(response)) {
            return response;
        }

        return JsonUtils.toJavaObject(response, responseType);
    }
}