/**
 * 
 */
package io.sipstack.example.netty.uas;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipResponse;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * @author jonas@jonasborjesson.com
 */
public final class SipHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

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
