/**
 * 
 */
package io.sipstack.example.netty.uas;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.sipstack.netty.codec.sip.SipMessageDecoder;

import java.net.InetSocketAddress;

/**
 * @author jonas@jonasborjesson.com
 * 
 */
public class Server {

    private final String ip;

    private final int port;

    public Server(final String ip, final int port) {
        this.ip = ip;
        this.port = port;
    }

    public void run() throws Exception {
        final EventLoopGroup group = new NioEventLoopGroup();
        try {
            final Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        protected void initChannel(final DatagramChannel ch) throws Exception {
                            final ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("decoder", new SipMessageDecoder());
                            pipeline.addLast("handler", new SipHandler());
                        }
                    });

            final InetSocketAddress socketAddress = new InetSocketAddress(this.ip, this.port);
            b.bind(socketAddress).sync().channel().closeFuture().await();
        } finally {
            group.shutdownGracefully();
        }
    }

    public static void main(final String[] args) throws Exception {
        new Server("127.0.0.1", 5060).run();
    }
}
