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
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.sipstack.netty.codec.sip.SipMessageDatagramDecoder;
import io.sipstack.netty.codec.sip.SipMessageEvent;
import io.sipstack.netty.codec.sip.SipMessageStreamDecoder;

import java.net.InetSocketAddress;

/**
 * Bootstrap netty and setup the sip message encoder/decoder for basic SIP support.
 * 
 * Note: you can't really call this a SIP stack since all it provides is basic framing and parsing
 * of SIP messages but it is the very first layer of a true SIP stack (see rfc3261 for the various
 * layers of SIP).
 * 
 * @author jonas@jonasborjesson.com
 */
public class SimpleSipStack {

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

    public SimpleSipStack(final SimpleChannelInboundHandler<SipMessageEvent> handler, final String ip, final int port) {
        this.ip = ip;
        this.port = port;

        this.bootstrap = createUDPListeningPoint(handler);
        this.serverBootstrap = createTCPListeningPoint(handler);
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

    private Bootstrap createUDPListeningPoint(final SimpleChannelInboundHandler<SipMessageEvent> handler) {
        final Bootstrap b = new Bootstrap();
        b.group(this.udpGroup)
        .channel(NioDatagramChannel.class)
        .handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(final DatagramChannel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("decoder", new SipMessageDatagramDecoder());
                pipeline.addLast("handler", handler);
            }
        });
        return b;
    }

    private ServerBootstrap createTCPListeningPoint(final SimpleChannelInboundHandler<SipMessageEvent> handler) {
        final ServerBootstrap b = new ServerBootstrap();

        b.group(this.bossGroup, this.workerGroup)
        .channel(NioServerSocketChannel.class)
        .childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(final SocketChannel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("decoder", new SipMessageStreamDecoder());
                pipeline.addLast("handler", handler);
            }
        })
        .option(ChannelOption.SO_BACKLOG, 128)
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
        .childOption(ChannelOption.SO_KEEPALIVE, true)
        .childOption(ChannelOption.TCP_NODELAY, true);
        return b;
    }

}
