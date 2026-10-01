package tech.powerjob.remote.mu;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import tech.powerjob.remote.framework.base.Address;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Connection manager for worker-side lazy connection to server
 *
 * @author claude
 * @since 2025/1/1
 */
@Slf4j
public class MuConnectionManager {

    private final EventLoopGroup workerGroup;
    private final ChannelManager channelManager;
    private final Object messageHandler; // Can be MuWorkerHandler or MuServerHandler
    private final Address localAddress;
    
    private final ConcurrentMap<String, Channel> serverConnections = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CompletableFuture<Channel>> pendingConnections = new ConcurrentHashMap<>();

    public MuConnectionManager(EventLoopGroup workerGroup, ChannelManager channelManager, 
                              Object messageHandler, Address localAddress) {
        this.workerGroup = workerGroup;
        this.channelManager = channelManager;
        this.messageHandler = messageHandler;
        this.localAddress = localAddress;
    }

    /**
     * Get or create connection to target address
     * @param targetAddress target address
     * @return CompletableFuture of channel
     */
    public CompletableFuture<Channel> getOrCreateConnection(Address targetAddress) {
        String key = targetAddress.getHost() + ":" + targetAddress.getPort();
        
        // Check if we already have an active connection
        Channel existingChannel = serverConnections.get(key);
        if (existingChannel != null && existingChannel.isActive()) {
            return CompletableFuture.completedFuture(existingChannel);
        }

        // Check if there's already a pending connection
        CompletableFuture<Channel> pendingConnection = pendingConnections.get(key);
        if (pendingConnection != null) {
            return pendingConnection;
        }

        // Create new connection
        CompletableFuture<Channel> connectionFuture = new CompletableFuture<>();
        pendingConnections.put(key, connectionFuture);

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline()
                            .addLast(new IdleStateHandler(0, 30, 0, TimeUnit.SECONDS))
                            .addLast(new MuMessageCodec())
                            .addLast((io.netty.channel.ChannelHandler) messageHandler);
                        
                        // Only add heartbeat handler for Worker connections
                        if (messageHandler instanceof MuWorkerHandler) {
                            ch.pipeline().addLast(new MuWorkerHeartbeatHandler(localAddress));
                        }
                    }
                });

            ChannelFuture future = bootstrap.connect(targetAddress.getHost(), targetAddress.getPort());
            future.addListener(f -> {
                pendingConnections.remove(key);
                if (f.isSuccess()) {
                    Channel channel = future.channel();
                    serverConnections.put(key, channel);
                    
                    // Remove connection when it becomes inactive
                    channel.closeFuture().addListener(closeFuture -> {
                        serverConnections.remove(key, channel);
                        log.info("[MuConnectionManager] Removed inactive server connection: {}", key);
                    });
                    
                    connectionFuture.complete(channel);
                    log.info("[MuConnectionManager] Connected to server: {}", key);
                } else {
                    connectionFuture.completeExceptionally(f.cause());
                    log.error("[MuConnectionManager] Failed to connect to server: {}", key, f.cause());
                }
            });
        } catch (Exception e) {
            pendingConnections.remove(key);
            connectionFuture.completeExceptionally(e);
            log.error("[MuConnectionManager] Error creating connection to server: {}", key, e);
        }

        return connectionFuture;
    }

    /**
     * Close all connections
     */
    public void closeAllConnections() {
        for (Channel channel : serverConnections.values()) {
            if (channel.isActive()) {
                channel.close();
            }
        }
        serverConnections.clear();
        
        // Complete all pending connections with exception
        for (CompletableFuture<Channel> future : pendingConnections.values()) {
            future.completeExceptionally(new RuntimeException("Connection manager is closing"));
        }
        pendingConnections.clear();
    }
}