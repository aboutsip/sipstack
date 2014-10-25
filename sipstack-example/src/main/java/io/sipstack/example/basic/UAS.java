/**
 * 
 */
package io.sipstack.example.basic;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.SipMessageDatagramDecoder;
import io.sipstack.netty.codec.sip.SipMessageEncoder;
import io.sipstack.netty.codec.sip.SipMessageEvent;

import java.net.InetSocketAddress;

/**
 * Basic mini sip stack for acting as an UAS. Run it and then use e.g. SIPp
 * (http://sipp.sourceforge.net) to test it out like so:
 * 
 * <pre>
 * sipp -sn uac 127.0.0.1:5060
 * </pre>
 * 
 * This will start SIPp using the 'uac' scenario, i.e., SIPp will act as a client and execute a call
 * setup/teardown flow, which is SIPs way of making a "phone call". 
 * 
 * @author jonas@jonasborjesson.com
 *
 */
@Sharable
public final class UAS extends SimpleChannelInboundHandler<SipMessageEvent> {

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent event) throws Exception {
        final SipMessage msg = event.getMessage();

        // just consume the ACK
        if (msg.isAck()) {
            return;
        }

        // for all other requests, just generate a 200 OK response.
        if (msg.isRequest()) {
            final SipResponse response = msg.createResponse(200);
            event.getConnection().send(response);
        }
    }

    public static void main(final String[] args) throws Exception {
        final UAS uas = new UAS();
        final EventLoopGroup udpGroup = new NioEventLoopGroup();

        final Bootstrap b = new Bootstrap();
        b.group(udpGroup)
        .channel(NioDatagramChannel.class)
        .handler(new ChannelInitializer<DatagramChannel>() {
            @Override
            protected void initChannel(final DatagramChannel ch) throws Exception {
                final ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("decoder", new SipMessageDatagramDecoder());
                pipeline.addLast("encoder", new SipMessageEncoder());
                pipeline.addLast("handler", uas);
            }
        });

        final InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 5060);
        b.bind(socketAddress).sync().channel().closeFuture().await();
    }

}
