/**
 * 
 */
package io.sipstack.netty.codec.sip;

import io.netty.channel.Channel;
import io.pkts.packet.sip.SipMessage;

import java.net.InetSocketAddress;

/**
 * @author jonas@jonasborjesson.com
 */
public final class TcpConnection extends AbstractConnection {

    /**
     * @param ctx
     * @param remote
     */
    // public TcpConnection(final ChannelHandlerContext ctx, final InetSocketAddress remote) {
    // super(ctx, remote);
    // }

    public TcpConnection(final Channel channel, final InetSocketAddress remote) {
        super(channel, remote);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final SipMessage msg) {
        channel().writeAndFlush(toByteBuf(msg));
    }

    @Override
    public boolean connect() {
        return true;
    }

}
