package io.sipstack.example.netty.sip.registrar;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.buffer.Buffer;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.address.URI;
import io.pkts.packet.sip.header.ContactHeader;
import io.pkts.packet.sip.header.ExpiresHeader;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipMessageEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class RegistrarHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

    /**
     * Our "location store". Normally you would hide this behind some interface that probably
     * persist to disk, as a distributed cache but for our purposes we'll just keep it here.
     * 
     * And yes, Google Guava Table is better suited but didn't want to pull in too many
     * dependences...
     */
    private final Map<String, List<Binding>> locationStore = new HashMap<String, List<Binding>>();

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent event) throws Exception {
        final Connection connection = event.getConnection();
        final SipMessage msg = event.getMessage();

        if (msg.isRequest() && msg.isRegister()) {
            final SipResponse response = processRegisterRequest(msg.toRequest());
            connection.send(response);
        } else if (msg.isRequest()) {
            // we only handle register requests so anything else is a no so send
            // back a 405 Method Not Allowed Method, which means that we
            // understood what the UAC is trying to tell us but in this context
            // we don't allow it. However, for ACK's, which doesn't have a
            // response we just ignore it silently.
            if (!msg.isAck()) {
                connection.send(msg.toRequest().createResponse(405));
            }
        } else {
            // Just to make things clear, not really needed for our simple
            // exmple.
            //
            // Since we are just handling incoming register requests,
            // we should never ever receive any responses of any kind.
            // The only way this could happen is if someone is simply
            // trying to attack us by sending spoofed responses,
            // in either case, we will simply ignore but in a real
            // server you probably at least want to take note of
            // where those responses are coming from an perhaps firewall
            // them off somehoe, by e.g. using iptables.
            // Check out fail2ban for how this can be done automaticlaly.
        }

    }

    /**
     * Section 10.3 in RFC3261 outlines how to process a register request. For the purpose of this
     * little exercise, we are skipping many steps just to keep things simple.
     * 
     * @param request
     */
    private SipResponse processRegisterRequest(final SipRequest request) {
        final SipURI requestURI = (SipURI) request.getRequestUri();
        final Buffer domain = requestURI.getHost();
        final SipURI aor = getAOR(request);

        // the aor is not allowed to register under this domain
        // generate a 404 according to specfication
        if (!validateDomain(domain, aor)) {
            return request.createResponse(404);
        }

        final Binding.Builder builder = Binding.with();
        builder.aor(aor);
        builder.expires(getExpires(request));
        builder.cseq(request.getCSeqHeader());

        // NOTE: this is also cheating. There may be multiple contacts
        // and they must all get processed but whatever...
        builder.contact(getContactURI(request));

        final Binding binding = builder.build();

        return request.createResponse(200);
    }

    /**
     * See RFC3261 of how it is actually supposed to be done but the short version is:
     * 
     * For the AOR, compare the contact URI of all known bidnings and update/create/delete as
     * needed.
     * 
     * 
     * @param binding
     * @return
     */
    private List<Binding> updateBindings(final Binding binding) {


    }

    private SipURI getContactURI(final SipRequest request) {
        final ContactHeader contact = request.getContactHeader();
        final URI uri = contact.getAddress().getURI();
        if (uri.isSipURI()) {
            return (SipURI) uri;
        }
        throw new IllegalArgumentException("We only allow SIP URI's in the ContactHeader");
    }

    private boolean validateDomain(final Buffer domain, final SipURI aor) {
        return aor.getHost().equals(domain);
    }

    private int getExpires(final SipRequest request) {
        final ContactHeader contact = request.getContactHeader();
        if (contact != null) {
            final Buffer value = contact.getParameter("expires");
            if (value != null) {
                return value.getInt(0);
            }
        }

        final ExpiresHeader expires = (ExpiresHeader) request.getHeader(ExpiresHeader.NAME);
        return expires.getExpires();
    }

    /**
     * The To-header contains the AOR (address-of-record) that the user wish to associate with the
     * contact information in the Contact-header. We must also convert the To-header into its
     * canonical form, which is the aor we will use as the key into the existing bindings.
     * 
     * @param request
     * @return
     */
    private SipURI getAOR(final SipRequest request) {
        final SipURI sipURI = (SipURI) request.getToHeader().getAddress().getURI();
        return SipURI.with().user(sipURI.getUser()).host(sipURI.getHost()).build();
    }

}
