/**
 * 
 */
package io.sipstack.example.netty.uas;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * A super simple UAS implementation.
 * 
 * @author jonas@jonasborjesson.com
 */
@Sharable
public final class UASHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent event) throws Exception {
        final SipMessage msg = event.getMessage();

        // just consume the ACK
        if (msg.isAck()) {
            return;
        }

        // for all requests, just generate a 200 OK response.
        if (msg.isRequest()) {
            final SipResponse response = msg.createResponse(200);
            event.getConnection().send(response);
        }
    }

}
