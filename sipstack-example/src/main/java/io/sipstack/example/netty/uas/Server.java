/**
 * 
 */
package io.sipstack.example.netty.uas;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.sipstack.netty.codec.sip.SipMessageDatagramDecoder;
import io.sipstack.netty.codec.sip.SipMessageStreamDecoder;

import java.net.InetSocketAddress;

/**
 * @author jonas@jonasborjesson.com
 * 
 */
public class Server {

    private final String ip;

    private final int port;

    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final EventLoopGroup udpGroup = new NioEventLoopGroup();

    /**
     * The TCP based bootstrap.
     */
    private final ServerBootstrap serverBootstrap;

    /**
     * Our UDP based bootstrap.
     */
    private final Bootstrap bootstrap;

    public Server(final String ip, final int port) {
        this.ip = ip;
        this.port = port;

        this.bootstrap = createUDPListeningPoint();
        this.serverBootstrap = createTCPListeningPoint();
    }

    public void run() throws Exception {
        try {
            final InetSocketAddress socketAddress = new InetSocketAddress(this.ip, this.port);
            this.bootstrap.bind(socketAddress).sync();
            this.serverBootstrap.bind(socketAddress).sync().channel().closeFuture().await();
        } finally {
            this.bossGroup.shutdownGracefully();
            this.workerGroup.shutdownGracefully();
            this.udpGroup.shutdownGracefully();
        }
    }

    private Bootstrap createUDPListeningPoint() {
        final Bootstrap b = new Bootstrap();
        b.group(this.udpGroup)
                .channel(NioDatagramChannel.class)
                .handler(new ChannelInitializer<DatagramChannel>() {
                    @Override
                    protected void initChannel(final DatagramChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new SipMessageDatagramDecoder());
                        pipeline.addLast("handler", new SipHandler());
                    }
                });
        return b;

    }

    private ServerBootstrap createTCPListeningPoint() {
        final ServerBootstrap b = new ServerBootstrap();

        b.group(this.bossGroup, this.workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(final SocketChannel ch) throws Exception {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("decoder", new SipMessageStreamDecoder());
                        pipeline.addLast("handler", new SipHandler());
                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true);
        return b;
    }

    public static void main(final String[] args) throws Exception {
        new Server("127.0.0.1", 5060).run();
    }
}
