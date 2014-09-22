/**
 * 
 */
package io.sipstack.example.netty.sip.uac;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.CSeqHeader;
import io.pkts.packet.sip.header.CallIdHeader;
import io.pkts.packet.sip.header.ContactHeader;
import io.pkts.packet.sip.header.FromHeader;
import io.pkts.packet.sip.header.ToHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.netty.codec.sip.SipMessageEvent;

/**
 * @author jonas@jonasborjesson.com
 */
@Sharable
public final class UACHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

    /**
     * Generating an ACK to a 2xx response is the same as any other subsequent request. However, a
     * subsequent request is generated in the context of what is known as a Dialog and since we
     * currently are completely stateless, this is not necessarily an easy thing to achieve but for
     * now we will ignore many of the details.
     * 
     * The general idea is this though:
     * 
     * <ul>
     * <li>The sub-sequent request is going to be sent to where ever the Contact header of the
     * response is pointing to. This is known as the remote-target.</li>
     * <li>CSeq is incremented by one EXCEPT for ACK's, which will have the same sequence number as
     * what it is "ack:ing". The method of the CSeq is "ACK" though.</li>
     * <li>Call-ID has to be the same</li>
     * <li>The remote and local tags has to be correctly preserved on the To- and From-headers</li>
     * <li></li>
     * <li></li>
     * </ul>
     * 
     * @param response
     * @return
     */
    private SipRequest generateAck(final SipResponse response) {

        final ContactHeader contact = response.getContactHeader();
        final SipURI requestURI = (SipURI) contact.getAddress().getURI();
        final ToHeader to = response.getToHeader();
        final FromHeader from = response.getFromHeader();
        // The contact of the response is where the remote party wishes
        // to receive future request. Since an ACK is a "future", or sub-sequent, request,
        // the request-uri of the ACK has to be whatever is in the
        // contact header of the response.

        // Since this is an ACK, the cseq should have the same cseq number as the response,
        // i.e., the same as the original INVITE that we are ACK:ing.
        final CSeqHeader cseq = CSeqHeader.with().cseq(response.getCSeqHeader().getSeqNumber()).method("ACK").build();
        final CallIdHeader callId = response.getCallIDHeader();

        // If there are Record-Route headers in the response, they must be
        // copied over as well otherwise the ACK will not go the correct
        // path through the network.
        // TODO

        // we also have to create a new Via header and as always, when creating
        // via header we need to fill out which ip, port and transport we are
        // coming in over. In SIP, unlike many other protocols, we can use
        // any transport protocol and it can actually change from message
        // to message but in this simple example we will just use the
        // same last time so we will only have to generate a new branch id
        final ViaHeader via = response.getViaHeader().clone();
        via.setBranch(ViaHeader.generateBranch());

        // now we have all the pieces so let's put it together
        final SipRequest.Builder builder = SipRequest.ack(requestURI);
        builder.from(from);
        builder.to(to);
        builder.callId(callId);
        builder.cseq(cseq);
        builder.via(via);
        return builder.build();
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent event) throws Exception {
        final SipMessage msg = event.getMessage();

        if (msg.isInvite() && msg.isResponse()) {
            final SipResponse response = msg.toResponse();
            if (response.isRinging()) {
                // yay, the other side is ringing! If you were to implement an
                // actual client you would start playing your favorite
                // ring tone now.
            } else if (response.isFinal()) {
                System.err.println("ok, final");
                final SipRequest ack = generateAck(response);
                System.err.println(ack);
                event.getConnection().send(ack);
            }
        }

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
