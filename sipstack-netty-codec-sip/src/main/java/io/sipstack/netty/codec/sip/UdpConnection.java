package io.sipstack.netty.codec.sip;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.pkts.packet.sip.SipMessage;

import java.net.InetSocketAddress;

/**
 * Encapsulates a
 * 
 * @author jonas@jonasborjesson.com
 */
public final class UdpConnection extends AbstractConnection {

    public UdpConnection(final ChannelHandlerContext ctx, final InetSocketAddress remoteAddress) {
        super(ctx, remoteAddress);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isUDP() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final SipMessage msg) {
        final DatagramPacket pkt = new DatagramPacket(toByteBuf(msg), getRemoteAddress());
        this.getContext().writeAndFlush(pkt);
    }

    @Override
    public boolean connect() {
        final ChannelFuture future = this.getContext().channel().connect(getRemoteAddress());
        try {
            future.sync();
            System.out.println("Connection was successful: " + future.isSuccess());
        } catch (final InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

}
