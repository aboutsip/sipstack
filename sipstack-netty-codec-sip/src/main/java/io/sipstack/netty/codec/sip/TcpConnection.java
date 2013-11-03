/**
 * 
 */
package io.sipstack.netty.codec.sip;

import io.netty.channel.ChannelHandlerContext;
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
    public TcpConnection(final ChannelHandlerContext ctx, final InetSocketAddress remote) {
        super(ctx, remote);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void send(final SipMessage msg) {
        this.getContext().writeAndFlush(toByteBuf(msg));
    }

}
