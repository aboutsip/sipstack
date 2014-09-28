package io.sipstack.example.netty.sip.proxyregistrar;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.buffer.Buffer;
import io.pkts.buffer.Buffers;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.address.URI;
import io.pkts.packet.sip.header.ContactHeader;
import io.pkts.packet.sip.header.ExpiresHeader;
import io.pkts.packet.sip.header.RouteHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.example.netty.sip.SimpleSipStack;
import io.sipstack.example.netty.sip.registrar.Binding;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipMessageEvent;

import java.util.List;

/**
 * 
 * @author jonas@jonasborjesson.com
 * 
 */
public final class ProxyRegistrarHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

    private final LocationService locationService = LocationService.getInstance();

    private SimpleSipStack stack;

    public void setStack(final SimpleSipStack stack) {
        this.stack = stack;
    }


    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent event) throws Exception {
        final Connection connection = event.getConnection();
        final SipMessage msg = event.getMessage();

        if (msg.isRequest() && msg.isOptions()) {
            // many clients will send out an OPTIONS request as a ping mechanism
            // and no reason to forward it so we will just response with a 200.
            connection.send(msg.toRequest().createResponse(200));
        } else if (msg.isRequest() && msg.isRegister()) {
            final SipResponse response = processRegisterRequest(msg.toRequest());
            connection.send(response);
        } else if (msg.isRequest()) {
            final SipURI next = getNextHop(msg.toRequest());
            if (next != null) {
                proxyTo(next, msg.toRequest());
            } else {
                connection.send(msg.toRequest().createResponse(404));
            }
        } else {
            // responses follow Via-headers to those are easy. Just pop the top-most via
            // since it is supposed to be us and then proxy to the second via. We should
            // check so that the via header we just popped indeed is pointing to us but
            // for now we will ignore these details.
            final SipResponse response = msg.toResponse();
            response.popViaHeader();
            proxy(response);
        }
    }

    private SipURI lookupLocation(final SipRequest request) {
        final SipURI requestURI = (SipURI) request.getRequestUri();
        final SipURI aor = SipURI.with().user(requestURI.getUser()).host(requestURI.getHost()).build();
        final List<Binding> bindings = this.locationService.getBindings(aor);

        // if there are no bindings for this AOR then return
        // 404 Not Found
        if (bindings == null || bindings.isEmpty()) {
            return null;
        }

        // if there are multiple bindings you should really fork the request
        // but since this is a simple example we will simply ignore any
        // but the first registration.
        return bindings.get(0).getContact();
    }

    /**
     * Calculate the next hop. In SIP, you can specify the path through the network you wish the
     * message to go and this is expressed through Route-headers and the request-uri.
     * 
     * Essentially, you check if there are {@link RouteHeader}s present, and if so, the top-most
     * {@link RouteHeader} is where you will proxy this message to and otherwise you will use the
     * request-uri as your target.
     * 
     * Of course, you also need to check whether perhaps you are the ultimate target but we will
     * ignore this for now. This is a simple proxy and if you send us bad traffic, bad things will
     * happen :-)
     * 
     * @param request
     * @return
     */
    private SipURI getNextHop(final SipRequest request) {

        if (request.isInitial() || request.isAck()) {
            return lookupLocation(request);
        }

        // normally you also need to check whether this route is
        // pointing to you and it it is you have to "consume" it
        // and look at the next one. As it stands now, if this
        // route is pointing to us and we will use it as the next
        // hop we will of course create a loop. For now, we will
        // ignore this.
        final RouteHeader route = request.getRouteHeader();
        if (route != null) {
            return (SipURI) route.getAddress().getURI();
        }

        return (SipURI) request.getRequestUri();
    }

    /**
     * Proxy the response to
     * 
     * @param via
     * @param msg
     */
    private void proxy(final SipResponse msg) {
        final ViaHeader via = msg.getViaHeader();
        final Connection connection = this.stack.connect(via.getHost(), via.getPort());
        connection.send(msg);
    }

    /**
     * Whenever we proxy a request we must also add a Via-header, which essentially says that the
     * request went "via this network address using this protocol". The {@link ViaHeader}s are used
     * for responses to find their way back the exact same path as the request took.
     * 
     * @param destination
     * @param msg
     */
    private void proxyTo(final SipURI destination, final SipRequest msg) {
        final int port = destination.getPort();
        final Connection connection = this.stack.connect(destination.getHost(), port == -1 ? 5060 : port);

        // SIP is pretty powerful but there are a lot of little details to get things working.
        // E.g., this sample application is acting as a stateless proxy and in order to
        // correctly relay re-transmissions or e.g. CANCELs we have to make sure to always
        // generate the same branch-id of the same request. Since a CANCEL will have the same
        // branch-id as the request it cancels, we must ensure we generate the same branch-id as
        // we did when we proxied the initial INVITE. If we don't, then the cancel will not be
        // matched by the "other" side and their phone wouldn't stop ringing.
        // SO, for this example, we'll just grab the previous value and append "-abc" to it so
        // now we are relying on the upstream element to do the right thing :-)
        //
        // See section 16.11 in RFC3263 for more information.
        final Buffer otherBranch = msg.getViaHeader().getBranch();
        final Buffer myBranch = Buffers.createBuffer(otherBranch.getReadableBytes() + 4);
        otherBranch.getBytes(myBranch);
        myBranch.write((byte) '-');
        myBranch.write((byte) 'a');
        myBranch.write((byte) 'b');
        myBranch.write((byte) 'c');
        final ViaHeader via = ViaHeader.with().host("10.0.1.28").port(5060).transportUDP().branch(myBranch).build();

        // This is how you should generate the branch parameter if you are a stateful proxy:
        // Note the ViaHeader.generateBranch()...
        // ViaHeader.with().host("10.0.1.28").port(5060).transportUDP().branch(ViaHeader.generateBranch()).build();

        msg.addHeaderFirst(via);

        try {
            connection.send(msg);
        } catch (final IndexOutOfBoundsException e) {
            System.out.println("this is the message:");
            System.out.println(msg.getRequestUri());
            System.out.println(msg.getMethod());
            System.out.println(msg);
            e.printStackTrace();
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
        builder.callId(request.getCallIDHeader());
        builder.expires(getExpires(request));
        builder.cseq(request.getCSeqHeader());

        // NOTE: this is also cheating. There may be multiple contacts
        // and they must all get processed but whatever...
        builder.contact(getContactURI(request));

        final Binding binding = builder.build();
        final List<Binding> currentBindings = this.locationService.updateBindings(binding);
        final SipResponse response = request.createResponse(200);
        currentBindings.forEach(b -> {
            final SipURI contactURI = b.getContact();
            contactURI.setParameter("expires", b.getExpires());
            final ContactHeader contact = ContactHeader.with(contactURI).build();
            response.addHeader(contact);
        });

        return response;
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

        final ExpiresHeader expires = request.getExpiresHeader();
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
