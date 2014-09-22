package io.sipstack.example.netty.sip.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.pkts.packet.sip.SipMessage;
import io.pkts.packet.sip.SipRequest;
import io.pkts.packet.sip.SipResponse;
import io.pkts.packet.sip.address.SipURI;
import io.pkts.packet.sip.header.RouteHeader;
import io.pkts.packet.sip.header.ViaHeader;
import io.sipstack.example.netty.sip.SimpleSipStack;
import io.sipstack.netty.codec.sip.Connection;
import io.sipstack.netty.codec.sip.SipMessageEvent;

public final class ProxyHandler extends SimpleChannelInboundHandler<SipMessageEvent> {

    private SimpleSipStack stack;

    public ProxyHandler() {
        // TODO Auto-generated constructor stub
    }

    public void setStack(final SimpleSipStack stack) {
        this.stack = stack;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final SipMessageEvent event) throws Exception {
        final SipMessage msg = event.getMessage();

        try {
            if (msg.isRequest()) {
                final SipURI next = getNextHop(msg.toRequest());
                proxyTo(next, msg.toRequest());
            } else {
                // responses follow Via-headers to those are easy. Just pop the top-most via
                // since it is supposed to be us and then proxy to the second via. We should
                // check so that the via header we just popped indeed is pointing to us but
                // for now we will ignore these details.
                final SipResponse response = msg.toResponse();
                response.popViaHeader();
                proxy(response);
            }
        } catch (final IllegalArgumentException e) {
            // Taking the lazy way out. If we find anything that isn't to our liking then
            // we will bail out with an IllegalArgumentException, which we will turn
            // into a 400 Bad Request. Of course, in a real world scenario you will
            // have to be a little more nuanced and send back more appropriate error
            // codes that really tells the story so that the UAC at least have a
            // fighting chance to figure out what it did wrong.
            e.printStackTrace();
            final SipResponse response = msg.toRequest().createResponse(400);
            event.getConnection().send(response);
        } catch (final Exception e) {
            // something went wrong, send a 500 back...
            e.printStackTrace();
            final SipResponse response = msg.toRequest().createResponse(500);
            event.getConnection().send(response);
        }
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
        final Connection connection = this.stack.connect(destination.getHost(), destination.getPort());
        final ViaHeader via =
                ViaHeader.with().host("127.0.0.1").port(5060).transportUDP().branch(ViaHeader.generateBranch()).build();
        msg.addHeaderFirst(via);
        connection.send(msg);
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
}
