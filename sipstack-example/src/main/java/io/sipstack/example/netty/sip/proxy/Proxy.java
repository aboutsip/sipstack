/**
 * 
 */
package io.sipstack.example.netty.sip.proxy;

import io.sipstack.example.netty.sip.SimpleSipStack;

/**
 * <p>
 * Acting as a proxy is a very common thing in SIP and this basic naive proxy example shows you how
 * to implement your own SIP proxy.
 * </p>
 * 
 * <p>
 * To test it out, run it and then use SIPp to send some traffic through. The setup will look like
 * this:
 * </p>
 * 
 * <p>
 * 
 * <pre>
 * +---------+                 +------------+                 +---------+
 * |   UAC   | --- INVITE -->  |    Proxy   | --- INVITE -->  |   UAS   |
 * |  (SIPp) | <-- 200 OK ---  | (our code) | <-- 200 OK ---  |  (SIPp) |
 * +---------+                 +------------+                 +---------+
 * </pre>
 * 
 * </p>
 * 
 * <p>
 * Our Proxy will sit in between the UAC and the UAS relaying SIP messages between those two
 * endpoints. Of course, the UAC and UAS can be any SIP capable software/device out there but in
 * order to test this example, using SIPp is the simplest option, which also will allow us to
 * performance test our Proxy.
 * </p>
 * 
 * <p>
 * </p>
 * 
 * Basic mini sip stack for acting as an UAS. Run it and then use e.g. SIPp
 * (http://sipp.sourceforge.net) to test it out like so:
 * 
 * @author jonas@jonasborjesson.com
 */
public final class Proxy {

    public static void main(final String[] args) throws Exception {
        final ProxyHandler handler = new ProxyHandler();
        final SimpleSipStack stack = new SimpleSipStack(handler, "127.0.0.1", 5060);
        handler.setStack(stack);
        stack.run();
    }

}
