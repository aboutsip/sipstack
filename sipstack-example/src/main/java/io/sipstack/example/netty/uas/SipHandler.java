/**
 * 
 */
package io.sipstack.example.netty.uas;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipMessageFactory;
import io.pkts.packet.sip.SipResponse;

/**
 * @author jonas@jonasborjesson.com
 */
public final class SipHandler extends SimpleChannelInboundHandler<SipMessage> {

    private final SipMessageFactory messageFactory;

    public SipHandler(final SipMessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessage msg) throws Exception {

        // just consume the ACK
        if (msg.isAck()) {
            return;
        }

        // for all requests, just generate a 200 OK response.
        if (msg.isRequest()) {
            final SipResponse response = this.messageFactory.createResponse(200, msg.toRequest());
            ctx.writeAndFlush(response);
        }

    }

}
