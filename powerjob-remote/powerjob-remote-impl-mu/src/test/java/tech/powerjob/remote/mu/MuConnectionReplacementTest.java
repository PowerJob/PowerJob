package tech.powerjob.remote.mu;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.jupiter.api.Test;
import tech.powerjob.remote.framework.base.Address;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MuConnectionReplacementTest {

    @Test
    void closingAnOldServerConnectionPreservesItsReplacement() throws Exception {
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup peerWorkers = new NioEventLoopGroup(1);
        EventLoopGroup clientWorkers = new NioEventLoopGroup(1);
        Channel peer = null;
        EmbeddedChannel replacement = new EmbeddedChannel();
        MuConnectionManager manager = new MuConnectionManager(clientWorkers, new ChannelManager(),
                new ChannelInboundHandlerAdapter(), new Address().setHost("127.0.0.1").setPort(0));
        try {
            peer = new ServerBootstrap().group(boss, peerWorkers).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) { }
                    }).bind("127.0.0.1", 0).sync().channel();
            Address address = new Address().setHost("127.0.0.1")
                    .setPort(((InetSocketAddress) peer.localAddress()).getPort());
            Channel old = manager.getOrCreateConnection(address).get(3, TimeUnit.SECONDS);
            String key = address.getHost() + ":" + address.getPort();
            ConcurrentMap<String, Channel> connections = connections(manager);
            connections.put(key, replacement);

            // Run the listener registered by a real successful connect after the replacement is installed.
            old.close().sync();
            clientWorkers.next().submit(() -> { }).get(3, TimeUnit.SECONDS);
            assertSame(replacement, connections.get(key));
        } finally {
            manager.closeAllConnections();
            replacement.finishAndReleaseAll();
            if (peer != null) peer.close().sync();
            shutdown(clientWorkers);
            shutdown(peerWorkers);
            shutdown(boss);
        }
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentMap<String, Channel> connections(MuConnectionManager manager) throws Exception {
        Field field = MuConnectionManager.class.getDeclaredField("serverConnections");
        field.setAccessible(true);
        return (ConcurrentMap<String, Channel>) field.get(manager);
    }

    private static void shutdown(EventLoopGroup group) throws Exception {
        assertTrue(group.shutdownGracefully(0, 2, TimeUnit.SECONDS).await(3, TimeUnit.SECONDS));
    }
}
