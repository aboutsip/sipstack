/**
 * 
 */
package io.sipstack.example.netty.uas;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.packet.sip.SipMessage;

/**
 * @author jonas@jonasborjesson.com
 */
public final class SipHandler extends SimpleChannelInboundHandler<SipMessage> {

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessage msg) throws Exception {
        System.out.println(msg);
    }

}
